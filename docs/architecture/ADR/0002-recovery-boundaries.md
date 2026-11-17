# ADR 0002 · Explicit file and cloud recovery

**Status:** Approved technical baseline.

**Decision:** Keep Room authoritative. M4 adds manual export and restore through a versioned snapshot file. M5 uses the same format for optional cloud snapshot recovery.

Manual file backup requires the driver to export and retain the file. It does not run automatically. Cloud recovery restores the last successfully uploaded snapshot. It does not provide real-time multi-device synchronization or merge edits. Changes made after an export or acknowledged upload require another backup.

Both restores validate the snapshot and require an empty ledger. Cloud writes check the previous acknowledged revision. A stale device can retain and export its local work without overwriting the cloud snapshot. This limits implementation and support work while protecting driver data. Reconsider synchronization if the pilot shows that concurrent device use is a recurring need.
