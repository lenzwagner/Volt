package com.lenz.tennisapp.data.local

import com.lenz.tennisapp.domain.model.Surface
import com.lenz.tennisapp.domain.model.TournamentCategory

data class TournamentCalendarEntry(
    val name: String,
    val location: String,
    val surface: Surface,
    val category: TournamentCategory,
    val startDate: String,
    val endDate: String,
    val keywords: List<String>
) {
    fun isActiveOn(date: String): Boolean = date >= startDate && date <= endDate
}

object WtaCalendar {

    val tournaments: List<TournamentCalendarEntry> = listOf(

        // ── GRAND SLAMS ──────────────────────────────────────────────────────────
        entry("Australian Open", "Melbourne, Australia", Surface.HARD, TournamentCategory.GRAND_SLAM, "2026-01-19", "2026-02-01", "australian open", "melbourne"),
        entry("Roland-Garros", "Paris, France", Surface.CLAY, TournamentCategory.GRAND_SLAM, "2026-05-25", "2026-06-07", "roland garros", "roland-garros", "french open"),
        entry("Wimbledon", "London, United Kingdom", Surface.GRASS, TournamentCategory.GRAND_SLAM, "2026-06-29", "2026-07-12", "wimbledon", "the championships"),
        entry("US Open", "New York, USA", Surface.HARD, TournamentCategory.GRAND_SLAM, "2026-08-31", "2026-09-13", "us open", "u.s. open", "new york"),

        // ── WTA 1000 ─────────────────────────────────────────────────────────────
        entry("Qatar TotalEnergies Open", "Doha, Qatar", Surface.HARD, TournamentCategory.WTA_1000, "2026-02-09", "2026-02-15", "qatar open", "doha", "qatar totalenergies"),
        entry("Dubai Duty Free Tennis Championships", "Dubai, UAE", Surface.HARD, TournamentCategory.WTA_1000, "2026-02-16", "2026-02-22", "dubai"),
        entry("BNP Paribas Open", "Indian Wells, USA", Surface.HARD, TournamentCategory.WTA_1000, "2026-03-02", "2026-03-15", "indian wells", "bnp paribas open"),
        entry("Miami Open", "Miami, USA", Surface.HARD, TournamentCategory.WTA_1000, "2026-03-16", "2026-03-29", "miami open", "miami"),
        entry("Mutua Madrid Open", "Madrid, Spain", Surface.CLAY, TournamentCategory.WTA_1000, "2026-04-20", "2026-05-03", "mutua madrid", "madrid open"),
        entry("Internazionali BNL d'Italia", "Rome, Italy", Surface.CLAY, TournamentCategory.WTA_1000, "2026-05-04", "2026-05-17", "internazionali", "rome", "roma", "italian open", "bnl"),
        entry("National Bank Open", "Toronto, Canada", Surface.HARD, TournamentCategory.WTA_1000, "2026-08-03", "2026-08-09", "national bank open", "toronto", "rogers cup"),
        entry("Cincinnati Open", "Cincinnati, USA", Surface.HARD, TournamentCategory.WTA_1000, "2026-08-10", "2026-08-16", "cincinnati open", "cincinnati", "western & southern"),
        entry("China Open", "Beijing, China", Surface.HARD, TournamentCategory.WTA_1000, "2026-09-28", "2026-10-11", "china open", "beijing"),
        entry("Wuhan Open", "Wuhan, China", Surface.HARD, TournamentCategory.WTA_1000, "2026-10-12", "2026-10-18", "wuhan open", "wuhan"),

        // ── WTA 500 ──────────────────────────────────────────────────────────────
        entry("United Cup", "Perth/Sydney, Australia", Surface.HARD, TournamentCategory.WTA_500, "2026-01-05", "2026-01-11", "united cup"),
        entry("Brisbane International", "Brisbane, Australia", Surface.HARD, TournamentCategory.WTA_500, "2026-01-05", "2026-01-11", "brisbane international", "brisbane"),
        entry("Adelaide International", "Adelaide, Australia", Surface.HARD, TournamentCategory.WTA_500, "2026-01-12", "2026-01-18", "adelaide international", "adelaide"),
        entry("Mubadala Abu Dhabi Open", "Abu Dhabi, UAE", Surface.HARD, TournamentCategory.WTA_500, "2026-02-02", "2026-02-08", "abu dhabi"),
        entry("Merida Open AKRON", "Merida, Mexico", Surface.HARD, TournamentCategory.WTA_500, "2026-02-23", "2026-03-01", "merida open", "merida"),
        entry("Credit One Charleston Open", "Charleston, USA", Surface.CLAY, TournamentCategory.WTA_500, "2026-04-06", "2026-04-12", "charleston open", "charleston"),
        entry("Upper Austria Ladies Linz", "Linz, Austria", Surface.CLAY, TournamentCategory.WTA_500, "2026-04-06", "2026-04-12", "linz"),
        entry("Porsche Tennis Grand Prix", "Stuttgart, Germany", Surface.CLAY, TournamentCategory.WTA_500, "2026-04-13", "2026-04-19", "porsche tennis grand prix", "stuttgart"),
        entry("Internationaux de Strasbourg", "Strasbourg, France", Surface.CLAY, TournamentCategory.WTA_500, "2026-05-18", "2026-05-24", "strasbourg"),
        entry("The HSBC Championships", "London, United Kingdom", Surface.GRASS, TournamentCategory.WTA_500, "2026-06-08", "2026-06-14", "hsbc championships", "queen", "queen's club"),
        entry("Berlin Tennis Open", "Berlin, Germany", Surface.GRASS, TournamentCategory.WTA_500, "2026-06-15", "2026-06-21", "berlin tennis open", "berlin"),
        entry("Bad Homburg Open", "Bad Homburg, Germany", Surface.GRASS, TournamentCategory.WTA_500, "2026-06-22", "2026-06-28", "bad homburg"),
        entry("Mubadala DC Open", "Washington DC, USA", Surface.HARD, TournamentCategory.WTA_500, "2026-07-27", "2026-08-02", "dc open", "washington"),
        entry("Abierto GNP Seguros", "Monterrey, Mexico", Surface.HARD, TournamentCategory.WTA_500, "2026-08-24", "2026-08-30", "monterrey", "gnp seguros"),
        entry("Guadalajara Open", "Guadalajara, Mexico", Surface.HARD, TournamentCategory.WTA_500, "2026-09-14", "2026-09-20", "guadalajara"),
        entry("Singapore Tennis Open", "Singapore", Surface.INDOOR_HARD, TournamentCategory.WTA_500, "2026-09-21", "2026-09-27", "singapore"),
        entry("Ningbo Open", "Ningbo, China", Surface.HARD, TournamentCategory.WTA_500, "2026-10-19", "2026-10-25", "ningbo"),
        entry("Toray Pan Pacific Open", "Tokyo, Japan", Surface.HARD, TournamentCategory.WTA_500, "2026-10-26", "2026-11-01", "toray pan pacific", "tokyo"),

        // ── WTA 250 ──────────────────────────────────────────────────────────────
        entry("ASB Classic", "Auckland, New Zealand", Surface.HARD, TournamentCategory.WTA_250, "2026-01-05", "2026-01-11", "asb classic", "auckland"),
        entry("Hobart International", "Hobart, Australia", Surface.HARD, TournamentCategory.WTA_250, "2026-01-12", "2026-01-18", "hobart international", "hobart"),
        entry("Transylvania Open", "Cluj-Napoca, Romania", Surface.INDOOR_HARD, TournamentCategory.WTA_250, "2026-02-02", "2026-02-08", "transylvania open", "cluj"),
        entry("Ostrava Open", "Ostrava, Czech Republic", Surface.INDOOR_HARD, TournamentCategory.WTA_250, "2026-02-02", "2026-02-08", "ostrava"),
        entry("ATX Open", "Austin, USA", Surface.HARD, TournamentCategory.WTA_250, "2026-02-23", "2026-03-01", "atx open", "austin"),
        entry("Colsanitas Cup", "Bogotá, Colombia", Surface.CLAY, TournamentCategory.WTA_250, "2026-03-30", "2026-04-05", "colsanitas", "bogota", "bogotá"),
        entry("Open Capfinances Rouen", "Rouen, France", Surface.CLAY, TournamentCategory.WTA_250, "2026-04-13", "2026-04-19", "rouen"),
        entry("Grand Prix Rabat", "Rabat, Morocco", Surface.CLAY, TournamentCategory.WTA_250, "2026-05-18", "2026-05-24", "rabat", "morocco", "lalla meryem"),
        entry("Libéma Open", "s'Hertogenbosch, Netherlands", Surface.GRASS, TournamentCategory.WTA_250, "2026-06-08", "2026-06-14", "libema open", "hertogenbosch", "rosmalen"),
        entry("Lexus Nottingham Open", "Nottingham, United Kingdom", Surface.GRASS, TournamentCategory.WTA_250, "2026-06-15", "2026-06-21", "lexus nottingham", "nottingham open", "nottingham wta", "nottingham"),
        entry("Lexus Eastbourne Open", "Eastbourne, United Kingdom", Surface.GRASS, TournamentCategory.WTA_250, "2026-06-22", "2026-06-28", "eastbourne"),
        entry("UniCredit Iasi Open", "Iasi, Romania", Surface.CLAY, TournamentCategory.WTA_250, "2026-07-13", "2026-07-19", "iasi"),
        entry("Athens Open", "Athens, Greece", Surface.HARD, TournamentCategory.WTA_250, "2026-07-13", "2026-07-19", "athens open", "athens"),
        entry("MSC Hamburg Ladies Open", "Hamburg, Germany", Surface.CLAY, TournamentCategory.WTA_250, "2026-07-20", "2026-07-26", "hamburg ladies", "hamburg"),
        entry("Livesport Prague Open", "Prague, Czech Republic", Surface.HARD, TournamentCategory.WTA_250, "2026-07-20", "2026-07-26", "prague open", "livesport prague"),
        entry("The Memphis Classic", "Memphis, USA", Surface.HARD, TournamentCategory.WTA_250, "2026-07-27", "2026-08-02", "memphis"),
        entry("SP Open", "São Paulo, Brazil", Surface.HARD, TournamentCategory.WTA_250, "2026-09-14", "2026-09-20", "sp open", "são paulo", "sao paulo"),
        entry("Korea Open", "Seoul, South Korea", Surface.HARD, TournamentCategory.WTA_250, "2026-09-21", "2026-09-27", "korea open", "seoul"),
        entry("Kinoshita Group Japan Open", "Osaka, Japan", Surface.HARD, TournamentCategory.WTA_250, "2026-10-19", "2026-10-25", "japan open", "osaka", "kinoshita"),
        entry("Guangzhou Open", "Guangzhou, China", Surface.HARD, TournamentCategory.WTA_250, "2026-10-26", "2026-11-01", "guangzhou"),
        entry("Chennai Open", "Chennai, India", Surface.HARD, TournamentCategory.WTA_250, "2026-11-02", "2026-11-08", "chennai"),
        entry("Prudential Hong Kong Tennis Open", "Hong Kong", Surface.HARD, TournamentCategory.WTA_250, "2026-11-02", "2026-11-08", "hong kong"),

        // ── WTA FINALS ───────────────────────────────────────────────────────────
        entry("WTA Finals", "Riyadh, Saudi Arabia", Surface.INDOOR_HARD, TournamentCategory.WTA_1000, "2026-11-09", "2026-11-15", "wta finals", "riyadh"),

        // ── WTA 125 ──────────────────────────────────────────────────────────────
        entry("Workday Canberra Open", "Canberra, Australia", Surface.HARD, TournamentCategory.WTA_125, "2026-01-05", "2026-01-11", "canberra"),
        entry("Philippine Women's Open", "Manila, Philippines", Surface.HARD, TournamentCategory.WTA_125, "2026-01-26", "2026-02-01", "manila", "philippine women"),
        entry("L&T Mumbai Open", "Mumbai, India", Surface.HARD, TournamentCategory.WTA_125, "2026-02-02", "2026-02-08", "mumbai"),
        entry("Oeiras Indoor 1", "Oeiras, Portugal", Surface.INDOOR_HARD, TournamentCategory.WTA_125, "2026-02-09", "2026-02-15", "oeiras 1", "jamor indoor"),
        entry("Open Arena Les Sables d'Olonne", "Les Sables d'Olonne, France", Surface.INDOOR_HARD, TournamentCategory.WTA_125, "2026-02-09", "2026-02-15", "sables d'olonne", "les sables"),
        entry("Dow Tennis Classic", "Midland, USA", Surface.INDOOR_HARD, TournamentCategory.WTA_125, "2026-02-16", "2026-02-22", "dow tennis classic", "midland"),
        entry("Oeiras Indoor 2", "Oeiras, Portugal", Surface.INDOOR_HARD, TournamentCategory.WTA_125, "2026-02-16", "2026-02-22", "oeiras 2"),
        entry("Megasaray Hotels Open 1", "Antalya, Turkey", Surface.CLAY, TournamentCategory.WTA_125, "2026-02-23", "2026-03-01", "megasaray"),
        entry("Megasaray Hotels Open 2", "Antalya, Turkey", Surface.CLAY, TournamentCategory.WTA_125, "2026-03-02", "2026-03-08", "antalya"),
        entry("Megasaray Hotels Open 3", "Antalya, Turkey", Surface.CLAY, TournamentCategory.WTA_125, "2026-03-09", "2026-03-15", "antalya 3"),
        entry("Austin 125", "Austin, USA", Surface.HARD, TournamentCategory.WTA_125, "2026-03-09", "2026-03-15", "austin 125"),
        entry("Dubrovnik Open", "Dubrovnik, Croatia", Surface.CLAY, TournamentCategory.WTA_125, "2026-03-23", "2026-03-29", "dubrovnik"),
        entry("Open Villa de Madrid", "Madrid, Spain", Surface.CLAY, TournamentCategory.WTA_125, "2026-04-06", "2026-04-12", "villa de madrid", "silverway"),
        entry("Oeiras Ladies Open", "Oeiras, Portugal", Surface.CLAY, TournamentCategory.WTA_125, "2026-04-13", "2026-04-19", "oeiras ladies"),
        entry("Oeiras Open CETO", "Oeiras, Portugal", Surface.CLAY, TournamentCategory.WTA_125, "2026-04-20", "2026-04-26", "oeiras ceto", "oeiras open ceto"),
        entry("L'Open 35 de Saint-Malo", "Saint-Malo, France", Surface.CLAY, TournamentCategory.WTA_125, "2026-04-20", "2026-04-26", "saint-malo", "saint malo"),
        entry("Catalonia Open Solgironès", "La Bisbal d'Empordà, Spain", Surface.CLAY, TournamentCategory.WTA_125, "2026-04-27", "2026-05-03", "catalonia open", "solgironès", "bisbal"),
        entry("Huzhou Open", "Huzhou, China", Surface.CLAY, TournamentCategory.WTA_125, "2026-04-27", "2026-05-03", "huzhou"),
        entry("Istanbul Open", "Istanbul, Turkey", Surface.CLAY, TournamentCategory.WTA_125, "2026-05-04", "2026-05-10", "istanbul open"),
        entry("Jiangxi Open", "Jiujiang, China", Surface.HARD, TournamentCategory.WTA_125, "2026-05-04", "2026-05-10", "jiangxi open", "jiujiang"),
        entry("Parma Ladies Open", "Parma, Italy", Surface.CLAY, TournamentCategory.WTA_125, "2026-05-11", "2026-05-17", "parma"),
        entry("Trophée Clarins", "Paris, France", Surface.CLAY, TournamentCategory.WTA_125, "2026-05-11", "2026-05-17", "clarins", "trophee clarins"),
        entry("Makarska Open", "Makarska, Croatia", Surface.CLAY, TournamentCategory.WTA_125, "2026-06-01", "2026-06-07", "makarska"),
        entry("Open delle Puglie", "Foggia, Italy", Surface.CLAY, TournamentCategory.WTA_125, "2026-06-01", "2026-06-07", "foggia", "puglie"),
        entry("Lexus Birmingham Open", "Birmingham, United Kingdom", Surface.GRASS, TournamentCategory.WTA_125, "2026-06-01", "2026-06-07", "birmingham"),
        entry("Lexus Ilkley Open", "Ilkley, United Kingdom", Surface.GRASS, TournamentCategory.WTA_125, "2026-06-08", "2026-06-14", "ilkley"),
        entry("Memorial Eugenio Fontana", "Modena, Italy", Surface.CLAY, TournamentCategory.WTA_125, "2026-06-08", "2026-06-14", "modena", "fontana"),
        entry("Internazionali Femminili Di Brescia", "Brescia, Italy", Surface.CLAY, TournamentCategory.WTA_125, "2026-06-15", "2026-06-21", "brescia"),
        entry("Figueira da Foz Ladies Open", "Figueira da Foz, Portugal", Surface.HARD, TournamentCategory.WTA_125, "2026-06-15", "2026-06-21", "figueira da foz"),
        entry("Nordea Open", "Bastad, Sweden", Surface.CLAY, TournamentCategory.WTA_125, "2026-06-15", "2026-06-21", "nordea open", "bastad", "båstad"),
        entry("Grand Est Open 88", "Contrexeville, France", Surface.CLAY, TournamentCategory.WTA_125, "2026-07-06", "2026-07-12", "contrexeville", "grand est open"),
        entry("Hall of Fame Open", "Newport, USA", Surface.GRASS, TournamentCategory.WTA_125, "2026-07-06", "2026-07-12", "hall of fame open", "newport"),
        entry("Enka Open", "Istanbul, Turkey", Surface.HARD, TournamentCategory.WTA_125, "2026-07-13", "2026-07-19", "enka open"),
        entry("KTC Ladies Open", "Kitzbühel, Austria", Surface.CLAY, TournamentCategory.WTA_125, "2026-07-13", "2026-07-19", "kitzbühel", "kitzbuhel", "ktc ladies"),
        entry("ATV Bancomat Tennis Open", "Rome, Italy", Surface.CLAY, TournamentCategory.WTA_125, "2026-07-13", "2026-07-19", "bancomat", "atv bancomat"),
        entry("Palermo Ladies Open", "Palermo, Italy", Surface.CLAY, TournamentCategory.WTA_125, "2026-07-27", "2026-08-02", "palermo"),
        entry("AXERIA Open", "Targu Mures, Romania", Surface.CLAY, TournamentCategory.WTA_125, "2026-07-27", "2026-08-02", "axeria", "targu mures"),
        entry("Odlum Brown VanOpen", "Vancouver, Canada", Surface.HARD, TournamentCategory.WTA_125, "2026-07-27", "2026-08-02", "vanopen", "vancouver"),
        entry("T-Mobile Polish Open", "Warsaw, Poland", Surface.HARD, TournamentCategory.WTA_125, "2026-08-03", "2026-08-09", "polish open", "warsaw"),
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
