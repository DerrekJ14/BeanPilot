package com.beanpilot.domain.model

data class CoffeeBean(
    val id: Long = 0,
    val roaster: String,
    val beanName: String,
    val origin: String? = null,
    val process: String? = null,
    val roastDate: String? = null,
    val notes: String? = null,
)
