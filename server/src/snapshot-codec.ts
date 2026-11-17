// Mirror BackupCodec, StrictBackupJson and BackupValidation in the M4 wire contract.
export const snapshotMaxBytes = 4 * 1024 * 1024;
export const snapshotFormat = 'snapshot-v1';
export const uuidPattern = /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/;

type NumberToken = { token: string };
type Value = string | NumberToken | boolean | null | Map<string, Value> | Value[];
type Row = Map<string, Value>;
export type SnapshotProblem = 'snapshot_invalid' | 'snapshot_incompatible';

class InvalidSnapshot extends Error {
    readonly code: SnapshotProblem;

    constructor(code: SnapshotProblem = 'snapshot_invalid') {
        super(code);
        this.code = code;
    }
}

function check(condition: unknown): asserts condition {
    if (!condition) throw new InvalidSnapshot();
}

const jsonString = /"(?:[^"\\\u0000-\u001f]|\\(?:["\\/bfnrt]|u[0-9a-fA-F]{4}))*"/y;
const jsonNumber = /-?(?:0|[1-9][0-9]*)(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?/y;
const longToken = /^-?(?:0|[1-9][0-9]*)$/;
const whitespace = /[ \t\r\n]*/y;
const longMin = -(1n << 63n);
const longMax = (1n << 63n) - 1n;

function readSnapshot(source: string): Value {
    let offset = 0;
    let count = 0;

    function peek() {
        whitespace.lastIndex = offset;
        whitespace.exec(source);
        offset = whitespace.lastIndex;
        return source[offset];
    }

    function take(char: string) {
        if (peek() !== char) return false;
        offset++;
        return true;
    }

    function text() {
        check(peek() === '"');
        jsonString.lastIndex = offset;
        const match = jsonString.exec(source);
        check(match);
        offset = jsonString.lastIndex;
        const value = JSON.parse(match[0]) as string;
        check(value.isWellFormed());
        return value;
    }

    function value(depth: number): Value {
        check(++count <= 350_000);
        switch (peek()) {
            case '{': {
                check(depth < 3);
                offset++;
                const object: Row = new Map();
                if (!take('}')) {
                    do {
                        const key = text();
                        check(!object.has(key) && object.size < 16 && take(':'));
                        object.set(key, value(depth + 1));
                    } while (take(','));
                    check(take('}'));
                }
                return object;
            }
            case '[': {
                check(depth < 3);
                offset++;
                const array: Value[] = [];
                if (!take(']')) {
                    do {
                        array.push(value(depth + 1));
                    } while (take(','));
                    check(take(']'));
                }
                return array;
            }
            case '"':
                return text();
            case 'n':
                check(source.startsWith('null', offset));
                offset += 4;
                return null;
            case 't':
                check(source.startsWith('true', offset));
                offset += 4;
                return true;
            case 'f':
                check(source.startsWith('false', offset));
                offset += 5;
                return false;
            default: {
                jsonNumber.lastIndex = offset;
                const match = jsonNumber.exec(source);
                check(match);
                offset = jsonNumber.lastIndex;
                return { token: match[0] };
            }
        }
    }

    const result = value(0);
    check(peek() === undefined);
    return result;
}

function row(value: Value, fields: readonly string[]) {
    check(value instanceof Map && value.size === fields.length && fields.every(field => value.has(field)));
    return value;
}

function collection(root: Row, key: string, limit: number, fields: readonly string[]) {
    const items = root.get(key);
    check(Array.isArray(items) && items.length <= limit);
    return items.map(item => row(item, fields));
}

function text(value: Value | undefined) {
    check(typeof value === 'string');
    return value;
}

function integer(value: Value | undefined) {
    // Decode original integer tokens without going through JavaScript Number.
    check(value && typeof value === 'object' && 'token' in value);
    check(value.token.length <= 20 && longToken.test(value.token));
    const integer = BigInt(value.token);
    check(integer >= longMin && integer <= longMax);
    return integer;
}

function id(value: Value | undefined) {
    const result = text(value);
    check(uuidPattern.test(result));
    return result;
}

function byId(rows: Row[]) {
    const result = new Map<string, Row>();
    for (const item of rows) {
        const key = id(item.get('id'));
        check(!result.has(key));
        result.set(key, item);
    }
    return result;
}

function gregorianDate(year: number, month: number, day: number) {
    const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
    const lengths = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
    return month >= 1 && month <= 12 && day >= 1 && day <= lengths[month - 1];
}

function civilDate(value: Value | undefined) {
    const date = text(value);
    const parts = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
    check(parts && +parts[1] > 0 && gregorianDate(+parts[1], +parts[2], +parts[3]));
    return date;
}

function canonicalInstant(value: Value | undefined) {
    // java.time.Instant.toString emits expanded signed years and 0, 3, 6 or 9 fractional digits.
    const parts = /^(\d{4}|-\d{4}|[+-][1-9]\d{4,9})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{3}|\d{6}|\d{9}))?Z$/.exec(text(value));
    check(parts);
    const [, year, month, day, hour, minute, second, fraction] = parts;
    check(year !== '-0000' && Math.abs(+year) <= 1_000_000_000 &&
        gregorianDate(+year, +month, +day) && +hour < 24 && +minute < 60 && +second < 60 &&
        (!fraction || fraction.slice(-3) !== '000'));
}

// Kotlin Char.isWhitespace combines Java isWhitespace and isSpaceChar.
function nonblank(value: Value | undefined) {
    check(/[^\u0009-\u000d\u001c-\u0020\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000]/.test(text(value)));
}

const envelope = ['formatVersion', 'exportedAt', 'currencyUnit', 'vehicles', 'entries', 'readings', 'reminders'];
const vehicleFields = ['id', 'kind', 'displayLabel', 'catalogKey', 'productionYear', 'productionYearCalendar'];
const entryFields = ['id', 'vehicleId', 'category', 'occurredOn', 'createdAtEpochMillis', 'notes', 'amountRials', 'fuelVolumeMl'];
const readingFields = ['id', 'vehicleId', 'occurredOn', 'kilometers', 'entryId'];
const reminderFields = ['id', 'vehicleId', 'title', 'dueOn', 'dueKilometers', 'state'];
const unpriced = new Set(['oil_change', 'repair', 'parts']);
const categories = new Set([...unpriced, 'fuel', 'insurance', 'wash', 'other']);

function validateSnapshot(value: Value) {
    check(value instanceof Map);
    if (integer(value.get('formatVersion')) !== 1n) throw new InvalidSnapshot('snapshot_incompatible');
    const root = row(value, envelope);
    if (text(root.get('currencyUnit')) !== 'legacy_irr') throw new InvalidSnapshot('snapshot_incompatible');
    canonicalInstant(root.get('exportedAt'));
    const vehicles = byId(collection(root, 'vehicles', 100, vehicleFields));
    const entries = byId(collection(root, 'entries', 20_000, entryFields));
    const readings = byId(collection(root, 'readings', 20_000, readingFields));
    const reminders = byId(collection(root, 'reminders', 5_000, reminderFields));

    for (const vehicle of vehicles.values()) {
        check(vehicle.get('kind') === 'car');
        nonblank(vehicle.get('displayLabel'));
        if (vehicle.get('catalogKey') !== null) nonblank(vehicle.get('catalogKey'));
        const year = vehicle.get('productionYear');
        const calendar = vehicle.get('productionYearCalendar');
        if (year === null) {
            check(calendar === null);
        } else {
            check(integer(year) >= 1n && integer(year) <= 9999n);
            check(calendar === 'solar_hijri' || calendar === 'gregorian');
        }
    }

    for (const entry of entries.values()) {
        check(vehicles.has(id(entry.get('vehicleId'))));
        civilDate(entry.get('occurredOn'));
        integer(entry.get('createdAtEpochMillis'));
        if (entry.get('notes') !== null) text(entry.get('notes'));
        const category = text(entry.get('category'));
        check(categories.has(category));
        const amount = entry.get('amountRials');
        const fuel = entry.get('fuelVolumeMl');
        check(amount === null ? unpriced.has(category) : integer(amount) > 0n);
        check(category === 'fuel' ? integer(fuel) > 0n : fuel === null);
    }

    const linked = new Set<string>();
    const mileage = [];
    for (const reading of readings.values()) {
        const vehicle = id(reading.get('vehicleId'));
        const day = civilDate(reading.get('occurredOn'));
        const km = integer(reading.get('kilometers'));
        check(vehicles.has(vehicle) && km >= 0n);
        if (reading.get('entryId') !== null) {
            const key = id(reading.get('entryId'));
            const entry = entries.get(key);
            check(entry && entry.get('vehicleId') === vehicle && entry.get('occurredOn') === day && !linked.has(key));
            linked.add(key);
        }
        mileage.push({ vehicle, day, km });
    }
    mileage.sort((left, right) => {
        if (left.vehicle !== right.vehicle) return left.vehicle < right.vehicle ? -1 : 1;
        return left.day === right.day ? 0 : left.day < right.day ? -1 : 1;
    });
    let vehicle = '';
    let day = '';
    let earlierMax = 0n;
    let dayMax = 0n;
    for (const reading of mileage) {
        if (vehicle !== reading.vehicle) {
            vehicle = reading.vehicle;
            day = '';
            dayMax = 0n;
        }
        if (day !== reading.day) {
            day = reading.day;
            earlierMax = dayMax;
            dayMax = 0n;
        }
        check(reading.km >= earlierMax);
        if (reading.km > dayMax) dayMax = reading.km;
    }

    for (const reminder of reminders.values()) {
        check(vehicles.has(id(reminder.get('vehicleId'))));
        nonblank(reminder.get('title'));
        check(['active', 'completed', 'canceled'].some(state => state === reminder.get('state')));
        if (reminder.get('dueOn') === null) {
            check(integer(reminder.get('dueKilometers')) >= 0n);
        } else {
            civilDate(reminder.get('dueOn'));
            check(reminder.get('dueKilometers') === null);
        }
    }
}

export function snapshotProblem(bytes: Buffer): SnapshotProblem | undefined {
    try {
        check(bytes.length <= snapshotMaxBytes);
        let source: string;
        try {
            source = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(bytes);
        } catch {
            return 'snapshot_invalid';
        }
        validateSnapshot(readSnapshot(source));
        return undefined;
    } catch (error) {
        if (error instanceof InvalidSnapshot) return error.code;
        throw error;
    }
}
