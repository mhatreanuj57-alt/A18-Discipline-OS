package com.a18.discipline

import android.app.Application
import androidx.room.Room
import com.a18.discipline.data.*

class DisciplineApp: Application() {
    val repository by lazy {
        Repository(this,Room.databaseBuilder(this,DisciplineDatabase::class.java,"a18-native.db").build())
    }
}
