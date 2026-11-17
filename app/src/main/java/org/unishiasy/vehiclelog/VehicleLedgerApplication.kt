package org.unishiasy.vehiclelog

import android.app.Application

class VehicleLedgerApplication : Application() {
    val container by lazy { AppContainer(this) }
}
