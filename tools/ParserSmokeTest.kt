import com.skyportalthor.app.data.FigureKind
import com.skyportalthor.app.data.SkylanderPathParser

fun main() {
    data class Case(
        val file: String,
        val path: List<String>,
        val name: String,
        val element: String,
        val kind: FigureKind,
        val type: String
    )

    val cases = listOf(
        Case("SSA_Spyro_S1.sky", listOf("01_Spyros_Adventure", "Magic", "Spyro"), "Spyro", "Magic", FigureKind.CHARACTER, "Skylander"),
        Case("SC_Lava_Lance_Eruptor_SuperCharger.sky", listOf("01_Spyros_Adventure", "Fire", "Eruptor"), "Lava Lance Eruptor", "Fire", FigureKind.CHARACTER, "SuperCharger"),
        Case("TT_Gearshift_TrapMaster.sky", listOf("04_Trap_Team", "Tech", "Gearshift"), "Gearshift", "Tech", FigureKind.CHARACTER, "Trap Master"),
        Case("TT_Trap_Fire_MASTER_BLANK.sky", listOf("90_Objets_Portail", "04_Trap_Team", "Traps", "Fire"), "Trap Fire", "Fire", FigureKind.TRAP, "Trap"),
        Case("SC_Hot_Streak_Vehicle_Land.sky", listOf("05_SuperChargers", "Vehicules", "Land", "Fire", "Hot_Streak"), "Hot Streak", "Fire", FigureKind.VEHICLE, "Véhicule"),
        Case("IMAG_Crystal_Magic_MASTER_BLANK.sky", listOf("90_Objets_Portail", "06_Imaginators", "Creation_Crystals", "Magic"), "Crystal Magic", "Magic", FigureKind.CREATION_CRYSTAL, "Creation Crystal")
    )

    cases.forEach { c ->
        val got = SkylanderPathParser.parse(c.file, c.path)
        check(got.name == c.name) { "name ${c.file}: ${got.name} != ${c.name}" }
        check(got.element == c.element) { "element ${c.file}: ${got.element} != ${c.element}" }
        check(got.kind == c.kind) { "kind ${c.file}: ${got.kind} != ${c.kind}" }
        check(got.typeLabel == c.type) { "type ${c.file}: ${got.typeLabel} != ${c.type}" }
    }
    println("Parser smoke test: ${cases.size} cases OK")
}
