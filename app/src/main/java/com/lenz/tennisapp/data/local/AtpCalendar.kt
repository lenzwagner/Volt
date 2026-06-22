package com.lenz.tennisapp.data.local

import com.lenz.tennisapp.domain.model.Surface
import com.lenz.tennisapp.domain.model.TournamentCategory

object AtpCalendar {

    val tournaments: List<TournamentCalendarEntry> = listOf(

        // ── GRAND SLAMS ──────────────────────────────────────────────────────────
        entry("Australian Open", "Melbourne, Australia", Surface.HARD, TournamentCategory.GRAND_SLAM, "2026-01-18", "2026-02-01", "australian open", "melbourne"),
        entry("Roland-Garros", "Paris, France", Surface.CLAY, TournamentCategory.GRAND_SLAM, "2026-05-24", "2026-06-07", "roland garros", "roland-garros", "french open"),
        entry("Wimbledon", "London, United Kingdom", Surface.GRASS, TournamentCategory.GRAND_SLAM, "2026-06-29", "2026-07-12", "wimbledon", "the championships"),
        entry("US Open", "New York, USA", Surface.HARD, TournamentCategory.GRAND_SLAM, "2026-08-31", "2026-09-13", "us open", "u.s. open", "new york"),

        // ── ATP MASTERS 1000 ─────────────────────────────────────────────────────
        entry("BNP Paribas Open", "Indian Wells, USA", Surface.HARD, TournamentCategory.ATP_MASTERS_1000, "2026-03-04", "2026-03-17", "indian wells", "bnp paribas open"),
        entry("Miami Open", "Miami, USA", Surface.HARD, TournamentCategory.ATP_MASTERS_1000, "2026-03-18", "2026-03-31", "miami open", "miami"),
        entry("Rolex Monte-Carlo Masters", "Monte-Carlo, Monaco", Surface.CLAY, TournamentCategory.ATP_MASTERS_1000, "2026-04-05", "2026-04-12", "monte carlo", "monte-carlo"),
        entry("Mutua Madrid Open", "Madrid, Spain", Surface.CLAY, TournamentCategory.ATP_MASTERS_1000, "2026-04-22", "2026-05-05", "mutua madrid", "madrid open"),
        entry("Internazionali BNL d'Italia", "Rome, Italy", Surface.CLAY, TournamentCategory.ATP_MASTERS_1000, "2026-05-06", "2026-05-19", "internazionali", "bnl d'italia", "bnl"),
        entry("National Bank Open", "Montreal, Canada", Surface.HARD, TournamentCategory.ATP_MASTERS_1000, "2026-08-02", "2026-08-12", "national bank open", "montreal", "rogers cup"),
        entry("Cincinnati Open", "Cincinnati, USA", Surface.HARD, TournamentCategory.ATP_MASTERS_1000, "2026-08-13", "2026-08-23", "cincinnati open", "cincinnati", "western & southern"),
        entry("Rolex Shanghai Masters", "Shanghai, China", Surface.HARD, TournamentCategory.ATP_MASTERS_1000, "2026-10-07", "2026-10-18", "shanghai masters", "shanghai"),
        entry("Rolex Paris Masters", "Paris, France", Surface.INDOOR_HARD, TournamentCategory.ATP_MASTERS_1000, "2026-11-02", "2026-11-08", "paris masters", "rolex paris"),

        // ── ATP 500 ──────────────────────────────────────────────────────────────
        entry("Dallas Open", "Dallas, USA", Surface.INDOOR_HARD, TournamentCategory.ATP_500, "2026-02-09", "2026-02-15", "dallas open", "dallas"),
        entry("ABN AMRO Open", "Rotterdam, Netherlands", Surface.INDOOR_HARD, TournamentCategory.ATP_500, "2026-02-09", "2026-02-15", "abn amro", "rotterdam"),
        entry("Qatar Exxonmobil Open", "Doha, Qatar", Surface.HARD, TournamentCategory.ATP_500, "2026-02-16", "2026-02-22", "qatar exxonmobil", "doha"),
        entry("Rio Open", "Rio de Janeiro, Brazil", Surface.CLAY, TournamentCategory.ATP_500, "2026-02-16", "2026-02-22", "rio open", "rio de janeiro"),
        entry("Abierto Mexicano Telcel", "Acapulco, Mexico", Surface.HARD, TournamentCategory.ATP_500, "2026-02-23", "2026-03-01", "acapulco", "mexicano telcel"),
        entry("Dubai Duty Free Tennis Championships", "Dubai, UAE", Surface.HARD, TournamentCategory.ATP_500, "2026-02-23", "2026-03-01", "dubai duty free", "dubai"),
        entry("Barcelona Open Banc Sabadell", "Barcelona, Spain", Surface.CLAY, TournamentCategory.ATP_500, "2026-04-13", "2026-04-19", "barcelona open", "barcelona"),
        entry("BMW Open", "Munich, Germany", Surface.CLAY, TournamentCategory.ATP_500, "2026-04-13", "2026-04-19", "bmw open", "munich", "münchen"),
        entry("Bitpanda Hamburg Open", "Hamburg, Germany", Surface.CLAY, TournamentCategory.ATP_500, "2026-05-17", "2026-05-23", "hamburg open", "bitpanda hamburg"),
        entry("Terra Wortmann Open", "Halle, Germany", Surface.GRASS, TournamentCategory.ATP_500, "2026-06-15", "2026-06-21", "terra wortmann", "halle"),
        entry("HSBC Championships", "London, United Kingdom", Surface.GRASS, TournamentCategory.ATP_500, "2026-06-15", "2026-06-21", "hsbc championships", "queen's club", "queen", "atp london", "london"),
        entry("Mubadala Citi DC Open", "Washington DC, USA", Surface.HARD, TournamentCategory.ATP_500, "2026-07-27", "2026-08-02", "dc open", "washington", "mubadala citi"),
        entry("Kinoshita Group Japan Open", "Tokyo, Japan", Surface.HARD, TournamentCategory.ATP_500, "2026-09-30", "2026-10-06", "japan open", "tokyo", "kinoshita"),
        entry("China Open", "Beijing, China", Surface.HARD, TournamentCategory.ATP_500, "2026-09-30", "2026-10-06", "china open", "beijing"),
        entry("Swiss Indoors Basel", "Basel, Switzerland", Surface.INDOOR_HARD, TournamentCategory.ATP_500, "2026-10-26", "2026-11-01", "swiss indoors", "basel"),
        entry("Erste Bank Open", "Vienna, Austria", Surface.INDOOR_HARD, TournamentCategory.ATP_500, "2026-10-26", "2026-11-01", "erste bank open", "vienna", "wien"),

        // ── ATP 250 ──────────────────────────────────────────────────────────────
        entry("Brisbane International", "Brisbane, Australia", Surface.HARD, TournamentCategory.ATP_250, "2026-01-05", "2026-01-11", "brisbane international", "brisbane"),
        entry("Bank of China Hong Kong Tennis Open", "Hong Kong", Surface.HARD, TournamentCategory.ATP_250, "2026-01-05", "2026-01-11", "hong kong tennis open", "hong kong"),
        entry("Adelaide International", "Adelaide, Australia", Surface.HARD, TournamentCategory.ATP_250, "2026-01-12", "2026-01-18", "adelaide international", "adelaide"),
        entry("ASB Classic", "Auckland, New Zealand", Surface.HARD, TournamentCategory.ATP_250, "2026-01-12", "2026-01-18", "asb classic", "auckland"),
        entry("Open Occitanie", "Montpellier, France", Surface.INDOOR_HARD, TournamentCategory.ATP_250, "2026-02-02", "2026-02-08", "open occitanie", "montpellier"),
        entry("IEB+ Argentina Open", "Buenos Aires, Argentina", Surface.CLAY, TournamentCategory.ATP_250, "2026-02-09", "2026-02-15", "argentina open", "buenos aires"),
        entry("Delray Beach Open", "Delray Beach, USA", Surface.HARD, TournamentCategory.ATP_250, "2026-02-16", "2026-02-22", "delray beach"),
        entry("BCI Seguros Chile Open", "Santiago, Chile", Surface.CLAY, TournamentCategory.ATP_250, "2026-02-23", "2026-03-01", "chile open", "santiago"),
        entry("Tiriac Open", "Bucharest, Romania", Surface.CLAY, TournamentCategory.ATP_250, "2026-03-30", "2026-04-05", "tiriac open", "bucharest"),
        entry("US Men's Clay Court Championship", "Houston, USA", Surface.CLAY, TournamentCategory.ATP_250, "2026-03-30", "2026-04-05", "clay court championship", "houston", "sarofim"),
        entry("Grand Prix Hassan II", "Marrakech, Morocco", Surface.CLAY, TournamentCategory.ATP_250, "2026-03-30", "2026-04-05", "grand prix hassan", "marrakech"),
        entry("Gonet Geneva Open", "Geneva, Switzerland", Surface.CLAY, TournamentCategory.ATP_250, "2026-05-17", "2026-05-23", "geneva open", "gonet", "genf"),
        entry("Libema Open", "s'Hertogenbosch, Netherlands", Surface.GRASS, TournamentCategory.ATP_250, "2026-06-08", "2026-06-14", "libema open", "hertogenbosch", "s-hertogenbosch"),
        entry("Boss Open", "Stuttgart, Germany", Surface.GRASS, TournamentCategory.ATP_250, "2026-06-08", "2026-06-14", "boss open"),
        entry("Mallorca Championships", "Mallorca, Spain", Surface.GRASS, TournamentCategory.ATP_250, "2026-06-21", "2026-06-27", "mallorca championships", "mallorca"),
        entry("Lexus Eastbourne Open", "Eastbourne, United Kingdom", Surface.GRASS, TournamentCategory.ATP_250, "2026-06-22", "2026-06-28", "eastbourne"),
        entry("Nordea Open", "Bastad, Sweden", Surface.CLAY, TournamentCategory.ATP_250, "2026-07-13", "2026-07-19", "nordea open", "bastad", "båstad"),
        entry("EFG Swiss Open Gstaad", "Gstaad, Switzerland", Surface.CLAY, TournamentCategory.ATP_250, "2026-07-13", "2026-07-19", "gstaad", "swiss open gstaad"),
        entry("Croatia Open Umag", "Umag, Croatia", Surface.CLAY, TournamentCategory.ATP_250, "2026-07-13", "2026-07-19", "umag", "croatia open"),
        entry("Generali Open", "Kitzbühel, Austria", Surface.CLAY, TournamentCategory.ATP_250, "2026-07-20", "2026-07-26", "generali open", "kitzbühel", "kitzbuhel"),
        entry("Millennium Estoril Open", "Estoril, Portugal", Surface.CLAY, TournamentCategory.ATP_250, "2026-07-20", "2026-07-26", "estoril open", "estoril"),
        entry("Mifel Tennis Open", "Los Cabos, Mexico", Surface.HARD, TournamentCategory.ATP_250, "2026-07-27", "2026-08-02", "los cabos", "mifel"),
        entry("Winston-Salem Open", "Winston-Salem, USA", Surface.HARD, TournamentCategory.ATP_250, "2026-08-23", "2026-08-29", "winston-salem", "winston salem"),
        entry("Chengdu Open", "Chengdu, China", Surface.HARD, TournamentCategory.ATP_250, "2026-09-23", "2026-09-29", "chengdu open", "chengdu"),
        entry("Lynk & Co Hangzhou Open", "Hangzhou, China", Surface.HARD, TournamentCategory.ATP_250, "2026-09-23", "2026-09-29", "hangzhou open", "hangzhou", "lynk"),
        entry("Almaty Open", "Almaty, Kazakhstan", Surface.INDOOR_HARD, TournamentCategory.ATP_250, "2026-10-19", "2026-10-25", "almaty open", "almaty"),
        entry("European Open Brussels", "Brussels, Belgium", Surface.INDOOR_HARD, TournamentCategory.ATP_250, "2026-10-19", "2026-10-25", "european open", "brussels", "brüssel"),
        entry("Grand Prix Auvergne-Rhône-Alpes", "Lyon, France", Surface.INDOOR_HARD, TournamentCategory.ATP_250, "2026-10-19", "2026-10-25", "grand prix auvergne", "lyon"),
        entry("BNP Paribas Nordic Open", "Stockholm, Sweden", Surface.INDOOR_HARD, TournamentCategory.ATP_250, "2026-11-08", "2026-11-14", "nordic open", "stockholm"),

        // ── ATP FINALS ───────────────────────────────────────────────────────────
        entry("Nitto ATP Finals", "Turin, Italy", Surface.INDOOR_HARD, TournamentCategory.ATP_MASTERS_1000, "2026-11-15", "2026-11-22", "nitto atp finals", "atp finals", "turin"),
    )

    fun findByName(apiName: String, today: String? = null): TournamentCalendarEntry? {
        val n = apiName.lowercase()
        val matches = tournaments.filter { entry -> entry.keywords.any { kw -> n.contains(kw) } }
        if (today != null) return matches.firstOrNull { it.isActiveOn(today) } ?: matches.firstOrNull()
        return matches.firstOrNull()
    }

    private fun entry(
        name: String,
        location: String,
        surface: Surface,
        category: TournamentCategory,
        startDate: String,
        endDate: String,
        vararg keywords: String
    ) = TournamentCalendarEntry(name, location, surface, category, startDate, endDate, keywords.toList())
}
