package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Column

interface Identity <T> {
    val id: Column<T>
}