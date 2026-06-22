package com.lenz.tennisapp.data.local

import com.lenz.tennisapp.domain.model.Surface
import com.lenz.tennisapp.domain.model.TournamentCategory

object AtpChallengerCalendar {

    val tournaments: List<TournamentCalendarEntry> = listOf(
        // ── WEEK 1 (05-JAN) ─────────────────────────────────────────────────────
        entry("Bengaluru 1", "Bengaluru, India", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-01-05", "2026-01-11", "bengaluru 1", "bangalore 1"),
        entry("Canberra Challenger", "Canberra, Australia", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-01-05", "2026-01-11", "canberra"),
        entry("Noumea Challenger", "Noumea, New Caledonia", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-01-05", "2026-01-11", "noumea"),
        entry("Nonthaburi 1", "Nonthaburi, Thailand", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-01-05", "2026-01-11", "nonthaburi 1"),
        entry("Nottingham 1", "Nottingham, Great Britain", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-01-05", "2026-01-11", "nottingham 1"),

        // ── WEEK 2 (12-JAN) ─────────────────────────────────────────────────────
        entry("Nonthaburi 2", "Nonthaburi, Thailand", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-01-12", "2026-01-18", "nonthaburi 2"),
        entry("Buenos Aires Challenger Jan", "Buenos Aires, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-01-12", "2026-01-18", "buenos aires jan"),
        entry("Glasgow Challenger", "Glasgow, Great Britain", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-01-12", "2026-01-18", "glasgow"),

        // ── WEEK 3 (19-JAN) ─────────────────────────────────────────────────────
        entry("Oeiras 1", "Oeiras, Portugal", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-01-19", "2026-01-25", "oeiras 1"),
        entry("Itajai Challenger", "Itajai, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-01-19", "2026-01-25", "itajai"),
        entry("Soma Bay Challenger", "Soma Bay, Egypt", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-01-19", "2026-01-25", "soma bay"),
        entry("Phan Thiet 1", "Phan Thiet, Vietnam", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-01-19", "2026-01-25", "phan thiet 1"),

        // ── WEEK 4 (26-JAN) ─────────────────────────────────────────────────────
        entry("Manama Challenger", "Manama, Bahrain", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-01-26", "2026-02-01", "manama"),
        entry("Quimper Challenger", "Quimper, France", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_125, "2026-01-26", "2026-02-01", "quimper"),
        entry("Concepcion Challenger", "Concepcion, Chile", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-01-26", "2026-02-01", "concepcion"),
        entry("San Diego Challenger", "San Diego, USA", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-01-26", "2026-02-01", "san diego challenger"),
        entry("Oeiras 2", "Oeiras, Portugal", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-01-26", "2026-02-01", "oeiras 2"),
        entry("Phan Thiet 2", "Phan Thiet, Vietnam", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-01-26", "2026-02-01", "phan thiet 2"),

        // ── WEEK 5 (02-FEB) ─────────────────────────────────────────────────────
        entry("Rosario Challenger", "Rosario, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-02-02", "2026-02-08", "rosario"),
        entry("Brisbane Challenger 1", "Brisbane, Australia", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-02", "2026-02-08", "brisbane 1"),
        entry("Cleveland Challenger", "Cleveland, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-02", "2026-02-08", "cleveland"),
        entry("Tenerife 1", "Tenerife, Spain", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-02", "2026-02-08", "tenerife 1"),
        entry("Cesenatico Challenger", "Cesenatico, Italy", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-02-02", "2026-02-08", "cesenatico"),
        entry("Koblenz Challenger", "Koblenz, Germany", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_50, "2026-02-02", "2026-02-08", "koblenz"),

        // ── WEEK 6 (09-FEB) ─────────────────────────────────────────────────────
        entry("Pau Challenger", "Pau, France", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_125, "2026-02-09", "2026-02-15", "pau challenger"),
        entry("Brisbane Challenger 2", "Brisbane, Australia", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-09", "2026-02-15", "brisbane 2"),
        entry("Tenerife 2", "Tenerife, Spain", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-09", "2026-02-15", "tenerife 2"),
        entry("Baton Rouge Challenger", "Baton Rouge, USA", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-02-09", "2026-02-15", "baton rouge"),
        entry("Chennai Challenger", "Chennai, India", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-02-09", "2026-02-15", "chennai challenger"),

        // ── WEEK 7 (16-FEB) ─────────────────────────────────────────────────────
        entry("Lille Challenger", "Lille, France", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_125, "2026-02-16", "2026-02-22", "lille"),
        entry("Metepec Challenger", "Metepec, Mexico", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-16", "2026-02-22", "metepec"),
        entry("New Delhi Challenger", "New Delhi, India", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-16", "2026-02-22", "new delhi"),
        entry("Tigre 1", "Tigre, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-02-16", "2026-02-22", "tigre 1"),

        // ── WEEK 8 (23-FEB) ─────────────────────────────────────────────────────
        entry("Saint-Brieuc Challenger", "Saint-Brieuc, France", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-02-23", "2026-03-01", "saint-brieuc", "saint brieuc"),
        entry("Lugano Challenger", "Lugano, Switzerland", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-23", "2026-03-01", "lugano"),
        entry("Pune Challenger", "Pune, India", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-02-23", "2026-03-01", "pune"),
        entry("Tigre 2", "Tigre, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-02-23", "2026-03-01", "tigre 2"),

        // ── WEEK 9 (02-MAR) ─────────────────────────────────────────────────────
        entry("Thionville Challenger", "Thionville, France", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-03-02", "2026-03-08", "thionville"),
        entry("Brasilia Challenger", "Brasilia, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-02", "2026-03-08", "brasilia"),
        entry("Kigali 1", "Kigali, Rwanda", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-02", "2026-03-08", "kigali 1"),
        entry("Hersonissos 1", "Hersonissos, Greece", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-03-02", "2026-03-08", "hersonissos 1"),

        // ── WEEK 10 (09-MAR) ────────────────────────────────────────────────────
        entry("Cap Cana Challenger", "Cap Cana, Dominican Republic", Surface.HARD, TournamentCategory.CHALLENGER_175, "2026-03-09", "2026-03-15", "cap cana"),
        entry("Phoenix Challenger", "Phoenix, USA", Surface.HARD, TournamentCategory.CHALLENGER_175, "2026-03-09", "2026-03-15", "phoenix"),
        entry("Kigali 2", "Kigali, Rwanda", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-03-09", "2026-03-15", "kigali 2"),
        entry("Cherbourg Challenger", "Cherbourg, France", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-03-09", "2026-03-15", "cherbourg"),
        entry("Santiago Challenger", "Santiago, Chile", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-09", "2026-03-15", "santiago challenger"),
        entry("Hersonissos 2", "Hersonissos, Greece", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-03-09", "2026-03-15", "hersonissos 2"),

        // ── WEEK 11 (16-MAR) ────────────────────────────────────────────────────
        entry("Asuncion Rakiura", "Asuncion, Paraguay", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-16", "2026-03-22", "asuncion rakiura", "asuncion 1"),
        entry("Cuernavaca Challenger", "Cuernavaca, Mexico", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-03-16", "2026-03-22", "cuernavaca"),
        entry("Murcia Challenger", "Murcia, Spain", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-16", "2026-03-22", "murcia"),
        entry("Zadar Challenger", "Zadar, Croatia", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-16", "2026-03-22", "zadar"),

        // ── WEEK 12 (23-MAR) ────────────────────────────────────────────────────
        entry("Morelia Challenger", "Morelia, Mexico", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-03-23", "2026-03-29", "morelia"),
        entry("Naples Challenger", "Naples, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-03-23", "2026-03-29", "naples challenger"),
        entry("Alicante Montemar", "Alicante, Spain", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-03-23", "2026-03-29", "alicante montemar"),
        entry("Sao Paulo Challenger Mar", "Sao Paulo, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-03-23", "2026-03-29", "sao paulo mar"),
        entry("Bucaramanga Challenger", "Bucaramanga, Colombia", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-03-23", "2026-03-29", "bucaramanga", "floridablanca"),
        entry("Split Challenger", "Split, Croatia", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-03-23", "2026-03-29", "split challenger"),
        entry("Yokkaichi Challenger", "Yokkaichi, Japan", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-03-23", "2026-03-29", "yokkaichi"),

        // ── WEEK 13 (30-MAR) ────────────────────────────────────────────────────
        entry("Menorca Challenger", "Menorca, Spain", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-03-30", "2026-04-05", "menorca"),
        entry("Barletta Challenger", "Barletta, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-30", "2026-04-05", "barletta"),
        entry("San Luis Potosi Challenger", "San Luis Potosi, Mexico", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-30", "2026-04-05", "san luis potosi"),
        entry("Sao Leopoldo Challenger", "Sao Leopoldo, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-03-30", "2026-04-05", "sao leopoldo"),
        entry("Miyazaki Challenger", "Miyazaki, Japan", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-03-30", "2026-04-05", "miyazaki"),

        // ── WEEK 14 (06-APR) ────────────────────────────────────────────────────
        entry("Mexico City Challenger", "Mexico City, Mexico", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-04-06", "2026-04-12", "mexico city"),
        entry("Monza Challenger", "Monza, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-04-06", "2026-04-12", "monza"),
        entry("Campinas Challenger", "Campinas, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-06", "2026-04-12", "campinas"),
        entry("Madrid Challenger Apr", "Madrid, Spain", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-06", "2026-04-12", "madrid challenger"),
        entry("Sarasota Challenger", "Sarasota, USA", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-06", "2026-04-12", "sarasota"),
        entry("Wuning 1", "Wuning, China", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-04-06", "2026-04-12", "wuning 1"),

        // ── WEEK 15 (13-APR) ────────────────────────────────────────────────────
        entry("Busan Challenger", "Busan, South Korea", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-04-13", "2026-04-19", "busan"),
        entry("Oeiras 3", "Oeiras, Portugal", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-04-13", "2026-04-19", "oeiras 3"),
        entry("Santa Cruz de la Sierra", "Santa Cruz, Bolivia", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-13", "2026-04-19", "santa cruz de la sierra"),
        entry("Tallahassee Challenger", "Tallahassee, USA", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-13", "2026-04-19", "tallahassee"),
        entry("Wuning 2", "Wuning, China", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-04-13", "2026-04-19", "wuning 2"),

        // ── WEEK 16 (20-APR) ────────────────────────────────────────────────────
        entry("Gwangju Challenger", "Gwangju, South Korea", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-04-20", "2026-04-26", "gwangju"),
        entry("Rome Challenger Apr", "Rome, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-20", "2026-04-26", "rome challenger apr"),
        entry("Savannah Challenger", "Savannah, USA", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-20", "2026-04-26", "savannah"),
        entry("Abidjan 1", "Abidjan, Ivory Coast", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-04-20", "2026-04-26", "abidjan 1"),
        entry("Shymkent 1", "Shymkent, Kazakhstan", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-04-20", "2026-04-26", "shymkent 1"),

        // ── WEEK 17 (27-APR) ────────────────────────────────────────────────────
        entry("Aix-en-Provence Challenger", "Aix-en-Provence, France", Surface.CLAY, TournamentCategory.CHALLENGER_175, "2026-04-27", "2026-05-03", "aix-en-provence", "aix en provence"),
        entry("Cagliari Challenger", "Cagliari, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_175, "2026-04-27", "2026-05-03", "cagliari"),
        entry("Mauthausen Challenger", "Mauthausen, Austria", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-04-27", "2026-05-03", "mauthausen"),
        entry("Jiujiang Challenger", "Jiujiang, China", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-04-27", "2026-05-03", "jiujiang"),
        entry("Ostrava Challenger", "Ostrava, Czech Republic", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-04-27", "2026-05-03", "ostrava challenger"),
        entry("Abidjan 2", "Abidjan, Ivory Coast", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-04-27", "2026-05-03", "abidjan 2"),
        entry("Shymkent 2", "Shymkent, Kazakhstan", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-04-27", "2026-05-03", "shymkent 2"),

        // ── WEEK 18 (04-MAY) ────────────────────────────────────────────────────
        entry("Wuxi Challenger", "Wuxi, China", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-05-04", "2026-05-10", "wuxi"),
        entry("Francavilla al Mare", "Francavilla, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-05-04", "2026-05-10", "francavilla"),
        entry("Brazzaville Challenger", "Brazzaville, Congo", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-05-04", "2026-05-10", "brazzaville"),
        entry("Santos Challenger", "Santos, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-05-04", "2026-05-10", "santos challenger"),

        // ── WEEK 19 (11-MAY) ────────────────────────────────────────────────────
        entry("Bordeaux Challenger", "Bordeaux, France", Surface.CLAY, TournamentCategory.CHALLENGER_175, "2026-05-11", "2026-05-17", "bordeaux"),
        entry("Valencia Challenger", "Valencia, Spain", Surface.CLAY, TournamentCategory.CHALLENGER_175, "2026-05-11", "2026-05-17", "valencia challenger"),
        entry("Oeiras 4", "Oeiras, Portugal", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-05-11", "2026-05-17", "oeiras 4"),
        entry("Tunis Challenger", "Tunis, Tunisia", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-05-11", "2026-05-17", "tunis"),
        entry("Zagreb Challenger", "Zagreb, Croatia", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-05-11", "2026-05-17", "zagreb"),
        entry("Bengaluru 2", "Bengaluru, India", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-05-11", "2026-05-17", "bengaluru 2", "bangalore 2"),
        entry("Cordoba Challenger", "Cordoba, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-05-11", "2026-05-17", "cordoba challenger"),

        // ── WEEK 20 (18-MAY) ────────────────────────────────────────────────────
        entry("Istanbul Istinye", "Istanbul, Turkey", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-05-18", "2026-05-24", "istanbul istinye"),
        entry("Bengaluru 3", "Bengaluru, India", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-05-18", "2026-05-24", "bengaluru 3", "bangalore 3"),

        // ── WEEK 21 (25-MAY) ────────────────────────────────────────────────────
        entry("Chisinau Challenger", "Chisinau, Moldova", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-05-25", "2026-05-31", "chisinau"),
        entry("Little Rock Challenger", "Little Rock, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-05-25", "2026-05-31", "little rock"),
        entry("Vicenza Challenger", "Vicenza, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-05-25", "2026-05-31", "vicenza"),
        entry("Cervia Challenger", "Cervia, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-05-25", "2026-05-31", "cervia"),
        entry("Centurion 1", "Centurion, South Africa", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-05-25", "2026-05-31", "centurion 1"),
        entry("Kosice Challenger", "Kosice, Slovakia", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-05-25", "2026-05-31", "kosice"),

        // ── WEEK 22 (01-JUN) ────────────────────────────────────────────────────
        entry("Birmingham Challenger", "Birmingham, Great Britain", Surface.GRASS, TournamentCategory.CHALLENGER_125, "2026-06-01", "2026-06-07", "birmingham challenger"),
        entry("Perugia Challenger", "Perugia, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-06-01", "2026-06-07", "perugia"),
        entry("Bad Rappenau Challenger", "Bad Rappenau, Germany", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-06-01", "2026-06-07", "bad rappenau"),
        entry("Prostejov Challenger", "Prostejov, Czech Republic", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-06-01", "2026-06-07", "prostejov"),
        entry("Tyler Challenger", "Tyler, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-06-01", "2026-06-07", "tyler"),
        entry("Centurion 2", "Centurion, South Africa", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-06-01", "2026-06-07", "centurion 2"),

        // ── WEEK 23 (08-JUN) ────────────────────────────────────────────────────
        entry("Ilkley Challenger", "Ilkley, Great Britain", Surface.GRASS, TournamentCategory.CHALLENGER_125, "2026-06-08", "2026-06-14", "ilkley"),
        entry("Bratislava Challenger", "Bratislava, Slovakia", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-06-08", "2026-06-14", "bratislava challenger"),
        entry("Lyon Challenger", "Lyon, France", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-06-08", "2026-06-14", "lyon challenger"),
        entry("Cattolica Challenger", "Cattolica, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-06-08", "2026-06-14", "cattolica"),
        entry("San Miguel de Tucuman", "San Miguel de Tucuman, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-06-08", "2026-06-14", "san miguel de tucuman", "tucuman"),

        // ── WEEK 24 (15-JUN) ────────────────────────────────────────────────────
        entry("Nottingham 2", "Nottingham, Great Britain", Surface.GRASS, TournamentCategory.CHALLENGER_125, "2026-06-15", "2026-06-21", "nottingham 2", "nottingham"),
        entry("Parma Challenger", "Parma, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-06-15", "2026-06-21", "parma challenger", "parma"),
        entry("Poznan Challenger", "Poznan, Poland", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-06-15", "2026-06-21", "poznan"),
        entry("Dublin Challenger", "Dublin, Ireland", Surface.GRASS, TournamentCategory.CHALLENGER_75, "2026-06-15", "2026-06-21", "dublin"),
        entry("Asuncion CIT", "Asuncion, Paraguay", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-06-15", "2026-06-21", "asuncion cit", "asuncion 2", "asuncion (cit)"),
        entry("Royan Challenger", "Royan, France", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-06-15", "2026-06-21", "royan"),

        // ── WEEK 25 (22-JUN) ────────────────────────────────────────────────────
        entry("Targu Mures Challenger", "Targu Mures, Romania", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-06-22", "2026-06-28", "targu mures"),
        entry("Plovdiv Challenger", "Plovdiv, Bulgaria", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-06-22", "2026-06-28", "plovdiv"),
        entry("Piracicaba Challenger", "Piracicaba, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-06-22", "2026-06-28", "piracicaba"),

        // ── WEEK 26 (29-JUN) ────────────────────────────────────────────────────
        entry("Brasov Challenger", "Brasov, Romania", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-06-29", "2026-07-05", "brasov"),
        entry("Cary Challenger", "Cary, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-06-29", "2026-07-05", "cary"),
        entry("Milan Challenger", "Milan, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-06-29", "2026-07-05", "milan challenger"),
        entry("Quito Challenger", "Quito, Ecuador", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-06-29", "2026-07-05", "quito"),
        entry("Troyes Challenger", "Troyes, France", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-06-29", "2026-07-05", "troyes"),

        // ── WEEK 27 (06-JUL) ────────────────────────────────────────────────────
        entry("Braunschweig Challenger", "Braunschweig, Germany", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-07-06", "2026-07-12", "braunschweig"),
        entry("Newport Challenger", "Newport, USA", Surface.GRASS, TournamentCategory.CHALLENGER_125, "2026-07-06", "2026-07-12", "newport challenger"),
        entry("Iasi Challenger", "Iasi, Romania", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-07-06", "2026-07-12", "iasi challenger"),
        entry("Bogota Challenger", "Bogota, Colombia", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-07-06", "2026-07-12", "bogota challenger"),
        entry("Trieste Challenger", "Trieste, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-07-06", "2026-07-12", "trieste"),
        entry("Liege Challenger", "Liege, Belgium", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-07-06", "2026-07-12", "liege"),
        entry("Nottingham 3", "Nottingham, Great Britain", Surface.GRASS, TournamentCategory.CHALLENGER_50, "2026-07-06", "2026-07-12", "nottingham 3"),

        // ── WEEK 28 (13-JUL) ────────────────────────────────────────────────────
        entry("Bunschoten Challenger", "Bunschoten, Netherlands", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-07-13", "2026-07-19", "bunschoten"),
        entry("Cardenans Challenger", "Cardenans, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-07-13", "2026-07-19", "cardenans"),
        entry("Granby Challenger", "Granby, Canada", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-07-13", "2026-07-19", "granby"),
        entry("Lincoln Challenger", "Lincoln, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-07-13", "2026-07-19", "lincoln"),
        entry("Pozoblanco Challenger", "Pozoblanco, Spain", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-07-13", "2026-07-19", "pozoblanco"),

        // ── WEEK 29 (20-JUL) ────────────────────────────────────────────────────
        entry("Bloomfield Hills Challenger", "Bloomfield Hills, USA", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-07-20", "2026-07-26", "bloomfield hills"),
        entry("Zug Challenger", "Zug, Switzerland", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-07-20", "2026-07-26", "zug"),
        entry("Segovia Challenger", "Segovia, Spain", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-07-20", "2026-07-26", "segovia"),
        entry("Tampere Challenger", "Tampere, Finland", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-07-20", "2026-07-26", "tampere"),
        entry("Winnipeg Challenger", "Winnipeg, Canada", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-07-20", "2026-07-26", "winnipeg"),

        // ── WEEK 30 (27-JUL) ────────────────────────────────────────────────────
        entry("San Marino Challenger", "San Marino", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-07-27", "2026-08-02", "san marino"),
        entry("Vancouver Challenger", "Vancouver, Canada", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-07-27", "2026-08-02", "vancouver challenger"),
        entry("Bonn Challenger", "Bonn, Germany", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-07-27", "2026-08-02", "bonn"),
        entry("Liberec Challenger", "Liberec, Czech Republic", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-07-27", "2026-08-02", "liberec"),
        entry("Centurion 3", "Centurion, South Africa", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-07-27", "2026-08-02", "centurion 3"),

        // ── WEEK 31 (03-AUG) ────────────────────────────────────────────────────
        entry("Hagen Challenger", "Hagen, Germany", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-08-03", "2026-08-09", "hagen"),
        entry("Grodzisk Mazowiecki", "Grodzisk Mazowiecki, Poland", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-03", "2026-08-09", "grodzisk"),
        entry("Lexington Challenger", "Lexington, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-03", "2026-08-09", "lexington"),
        entry("Centurion 4", "Centurion, South Africa", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-03", "2026-08-09", "centurion 4"),
        entry("Istanbul Enka", "Istanbul, Turkey", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-08-03", "2026-08-09", "istanbul enka"),

        // ── WEEK 32 (10-AUG) ────────────────────────────────────────────────────
        entry("Brownsburg Challenger", "Brownsburg, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-10", "2026-08-16", "brownsburg"),
        entry("Hamburg Challenger", "Hamburg, Germany", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-08-10", "2026-08-16", "hamburg challenger"),
        entry("Todi Challenger", "Todi, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-08-10", "2026-08-16", "todi"),
        entry("Astana Challenger", "Astana, Kazakhstan", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-08-10", "2026-08-16", "astana challenger"),

        // ── WEEK 33 (17-AUG) ────────────────────────────────────────────────────
        entry("Cancun Challenger", "Cancun, Mexico", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-08-17", "2026-08-23", "cancun"),
        entry("Quebec City Challenger", "Quebec City, Canada", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-08-17", "2026-08-23", "quebec city"),
        entry("Kingston Challenger 1", "Kingston, Jamaica", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-17", "2026-08-23", "kingston 1"),
        entry("Prague Challenger", "Prague, Czech Republic", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-08-17", "2026-08-23", "prague challenger"),
        entry("Roehampton 1", "Roehampton, Great Britain", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-08-17", "2026-08-23", "roehampton 1"),
        entry("Sion Challenger", "Sion, Switzerland", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-08-17", "2026-08-23", "sion challenger"),
        entry("Tashkent Challenger", "Tashkent, Uzbekistan", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-08-17", "2026-08-23", "tashkent challenger"),

        // ── WEEK 34 (24-AUG) ────────────────────────────────────────────────────
        entry("Kingston Challenger 2", "Kingston, Jamaica", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-24", "2026-08-30", "kingston 2"),
        entry("Augsburg Challenger", "Augsburg, Germany", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-08-24", "2026-08-30", "augsburg"),
        entry("Roehampton 2", "Roehampton, Great Britain", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-08-24", "2026-08-30", "roehampton 2"),

        // ── WEEK 35 (31-AUG) ────────────────────────────────────────────────────
        entry("Como Challenger", "Como, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-08-31", "2026-09-06", "como"),
        entry("Manacor Challenger", "Manacor, Spain", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-31", "2026-09-06", "manacor"),
        entry("Porto CTP Challenger", "Porto, Portugal", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-08-31", "2026-09-06", "porto ctp"),
        entry("Zhangjiagang Challenger", "Zhangjiagang, China", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-08-31", "2026-09-06", "zhangjiagang"),

        // ── WEEK 36 (07-SEP) ────────────────────────────────────────────────────
        entry("Genoa Nuova Valletta", "Genoa, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-09-07", "2026-09-13", "genoa nuova", "nuova valletta"),
        entry("Seville Challenger", "Seville, Spain", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-09-07", "2026-09-13", "seville", "sevilla"),
        entry("Shanghai Challenger", "Shanghai, China", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-09-07", "2026-09-13", "shanghai challenger"),
        entry("Tulln Challenger", "Tulln, Austria", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-09-07", "2026-09-13", "tulln"),
        entry("Cassis Challenger", "Cassis, France", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-09-07", "2026-09-13", "cassis"),
        entry("Istanbul TED Club", "Istanbul, Turkey", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-09-07", "2026-09-13", "istanbul ted"),
        entry("Phan Thiet 3", "Phan Thiet, Vietnam", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-09-07", "2026-09-13", "phan thiet 3"),

        // ── WEEK 37 (14-SEP) ────────────────────────────────────────────────────
        entry("Szczecin Challenger", "Szczecin, Poland", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-09-14", "2026-09-20", "szczecin"),
        entry("Tiburon Challenger", "Tiburon, USA", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-09-14", "2026-09-20", "tiburon"),
        entry("Guangzhou Huangpu", "Guangzhou, China", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-09-14", "2026-09-20", "guangzhou huangpu"),
        entry("Rennes Challenger", "Rennes, France", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-09-14", "2026-09-20", "rennes"),
        entry("Biella Challenger", "Biella, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-09-14", "2026-09-20", "biella"),
        entry("Phan Thiet 4", "Phan Thiet, Vietnam", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-09-14", "2026-09-20", "phan thiet 4"),

        // ── WEEK 38 (21-SEP) ────────────────────────────────────────────────────
        entry("Saint-Tropez Challenger", "Saint-Tropez, France", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-09-21", "2026-09-27", "saint-tropez", "saint tropez"),
        entry("Buenos Aires Challenger Sep", "Buenos Aires, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-09-21", "2026-09-27", "buenos aires sep"),
        entry("Genoa Park Tennis", "Genoa, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-09-21", "2026-09-27", "genoa park", "park tennis training"),

        // ── WEEK 39 (28-SEP) ────────────────────────────────────────────────────
        entry("Porto Monte Aventino", "Porto, Portugal", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-09-28", "2026-10-04", "porto monte aventino", "monte aventino"),
        entry("Jingshan Challenger", "Jingshan, China", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-09-28", "2026-10-04", "jingshan"),
        entry("Columbus Challenger", "Columbus, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-09-28", "2026-10-04", "columbus"),
        entry("Curitiba Challenger", "Curitiba, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-09-28", "2026-10-04", "curitiba"),
        entry("Mouilleron-le-Captif", "Mouilleron-le-Captif, France", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-09-28", "2026-10-04", "mouilleron"),
        entry("Bari Challenger", "Bari, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-09-28", "2026-10-04", "bari challenger"),

        // ── WEEK 40 (05-OCT) ────────────────────────────────────────────────────
        entry("Villena Challenger", "Villena, Spain", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-10-05", "2026-10-11", "villena"),
        entry("Antofagasta Challenger", "Antofagasta, Chile", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-10-05", "2026-10-11", "antofagasta"),
        entry("Braga Challenger", "Braga, Portugal", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-10-05", "2026-10-11", "braga"),
        entry("Palermo Challenger", "Palermo, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-10-05", "2026-10-11", "palermo challenger"),

        // ── WEEK 41 (12-OCT) ────────────────────────────────────────────────────
        entry("Jinan Challenger", "Jinan, China", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-10-12", "2026-10-18", "jinan"),
        entry("Olbia Challenger", "Olbia, Italy", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-10-12", "2026-10-18", "olbia"),
        entry("Maia Challenger", "Maia, Portugal", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-10-12", "2026-10-18", "maia challenger"),
        entry("Roanne Challenger", "Roanne, France", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-10-12", "2026-10-18", "roanne"),
        entry("Cali Challenger", "Cali, Colombia", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-10-12", "2026-10-18", "cali challenger"),
        entry("Catania Challenger", "Catania, Italy", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-10-12", "2026-10-18", "catania"),

        // ── WEEK 42 (19-OCT) ────────────────────────────────────────────────────
        entry("Lisbon Challenger", "Lisbon, Portugal", Surface.CLAY, TournamentCategory.CHALLENGER_100, "2026-10-19", "2026-10-25", "lisbon"),
        entry("Alicante Torrevieja", "Alicante, Spain", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-10-19", "2026-10-25", "alicante torrevieja"),
        entry("Guangzhou Nansha", "Guangzhou, China", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-10-19", "2026-10-25", "guangzhou nansha"),
        entry("Santa Cruz de la Sierra 2", "Santa Cruz, Bolivia", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-10-19", "2026-10-25", "santa cruz 2"),

        // ── WEEK 43 (26-OCT) ────────────────────────────────────────────────────
        entry("Seoul Challenger", "Seoul, South Korea", Surface.HARD, TournamentCategory.CHALLENGER_125, "2026-10-26", "2026-11-01", "seoul challenger"),
        entry("Brest Challenger", "Brest, France", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-10-26", "2026-11-01", "brest challenger"),
        entry("Sioux Falls Challenger", "Sioux Falls, USA", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-10-26", "2026-11-01", "sioux falls"),
        entry("Guayaquil Challenger", "Guayaquil, Ecuador", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-10-26", "2026-11-01", "guayaquil"),
        entry("Ningbo Challenger", "Ningbo, China", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-10-26", "2026-11-01", "ningbo challenger"),
        entry("Gaborone Challenger", "Gaborone, Botswana", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-10-26", "2026-11-01", "gaborone"),

        // ── WEEK 44 (02-NOV) ────────────────────────────────────────────────────
        entry("Bratislava 2 Challenger", "Bratislava, Slovakia", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_125, "2026-11-02", "2026-11-08", "bratislava 2"),
        entry("Costa do Sauipe Challenger", "Costa do Sauipe, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_125, "2026-11-02", "2026-11-08", "costa do sauipe"),
        entry("Taipei Challenger", "Taipei, Taiwan", Surface.HARD, TournamentCategory.CHALLENGER_100, "2026-11-02", "2026-11-08", "taipei"),
        entry("Charlottesville Challenger", "Charlottesville, USA", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_75, "2026-11-02", "2026-11-08", "charlottesville"),
        entry("Villa Maria Challenger", "Villa Maria, Argentina", Surface.CLAY, TournamentCategory.CHALLENGER_50, "2026-11-02", "2026-11-08", "villa maria"),

        // ── WEEK 45 (09-NOV) ────────────────────────────────────────────────────
        entry("Orleans Challenger", "Orleans, France", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_125, "2026-11-09", "2026-11-15", "orleans"),
        entry("Knoxville Challenger", "Knoxville, USA", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-09", "2026-11-15", "knoxville"),
        entry("Matsuyama Challenger", "Matsuyama, Japan", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-09", "2026-11-15", "matsuyama"),
        entry("Lima Challenger", "Lima, Peru", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-11-09", "2026-11-15", "lima challenger"),
        entry("Brisbane Challenger 3", "Brisbane, Australia", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-09", "2026-11-15", "brisbane 3"),
        entry("Monastir 1", "Monastir, Tunisia", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-11-09", "2026-11-15", "monastir 1"),

        // ── WEEK 46 (16-NOV) ────────────────────────────────────────────────────
        entry("Helsinki Challenger", "Helsinki, Finland", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_125, "2026-11-16", "2026-11-22", "helsinki"),
        entry("Metz Challenger", "Metz, France", Surface.INDOOR_HARD, TournamentCategory.CHALLENGER_125, "2026-11-16", "2026-11-22", "metz challenger"),
        entry("Montevideo Challenger", "Montevideo, Uruguay", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-11-16", "2026-11-22", "montevideo"),
        entry("Kobe Challenger", "Kobe, Japan", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-16", "2026-11-22", "kobe"),
        entry("Drummondville Challenger", "Drummondville, Canada", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-16", "2026-11-22", "drummondville"),
        entry("Sydney Challenger", "Sydney, Australia", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-16", "2026-11-22", "sydney challenger"),
        entry("Champaign Challenger", "Champaign, USA", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-11-16", "2026-11-22", "champaign"),

        // ── WEEK 47 (23-NOV) ────────────────────────────────────────────────────
        entry("Florianopolis Challenger", "Florianopolis, Brazil", Surface.CLAY, TournamentCategory.CHALLENGER_75, "2026-11-23", "2026-11-29", "florianopolis"),
        entry("Playford Challenger", "Playford, Australia", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-23", "2026-11-29", "playford"),
        entry("Yokohama Challenger", "Yokohama, Japan", Surface.HARD, TournamentCategory.CHALLENGER_75, "2026-11-23", "2026-11-29", "yokohama"),
        entry("Monastir 2", "Monastir, Tunisia", Surface.HARD, TournamentCategory.CHALLENGER_50, "2026-11-23", "2026-11-29", "monastir 2"),
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
