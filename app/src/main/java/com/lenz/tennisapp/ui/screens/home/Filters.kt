package com.lenz.tennisapp.ui.screens.home

enum class TourFilter(val label: String) {
    ALL("Alle"), ATP("ATP"), WTA("WTA");
    override fun toString() = label
}

enum class FormatFilter(val label: String) {
    ALL("Alle"), SINGLES("Singles"), DOUBLES("Doubles");
    override fun toString() = label
}

enum class CategoryFilter(val label: String) {
    ALL("Alle"), 
    GRAND_SLAM("Grand Slam"), 
    MASTERS_1000("Masters 1000"), 
    F500("500er"), 
    F250("250er"), 
    CHALLENGER("Challenger");
    override fun toString() = label
}
