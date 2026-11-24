package co.biohub.cientifico.arquitectura;

import co.biohub.cientifico.dominio.VersionHallazgo;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * ASR-4 / HU-17: ninguna clase puede saltarse la guardia de ubicación. Si alguien agrega código que
 * lee coordenadas exactas por otro camino, estas pruebas fallan.
 */
class GuardiaUbicacionArquitecturaTest {

    private static JavaClasses clases;

    @BeforeAll
    static void importarClases() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("co.biohub.cientifico");
    }

    @Test
    void soloLaGuardiaUsaElProveedorDeUbicacion() {
        noClasses().that().haveNameNotMatching(".*GuardiaUbicacion.*")
                .and().haveNameNotMatching(".*ProveedorUbicacion.*")
                .should().dependOnClassesThat().haveNameMatching(".*ProveedorUbicacion.*")
                .because("la guardia es el único punto que decide quién ve coordenadas exactas")
                .check(clases);
    }

    @Test
    void soloElProveedorYElServicioDeHallazgosLeenLaLatitud() {
        noClasses().that().haveNameNotMatching(".*ProveedorUbicacion.*")
                .and().haveNameNotMatching(".*HallazgoService.*")
                .should().callMethod(VersionHallazgo.class, "getLatitud")
                .because("la latitud exacta solo sale por el proveedor de ubicación")
                .check(clases);
    }

    @Test
    void soloElProveedorYElServicioDeHallazgosLeenLaLongitud() {
        noClasses().that().haveNameNotMatching(".*ProveedorUbicacion.*")
                .and().haveNameNotMatching(".*HallazgoService.*")
                .should().callMethod(VersionHallazgo.class, "getLongitud")
                .because("la longitud exacta solo sale por el proveedor de ubicación")
                .check(clases);
    }

    @Test
    void lasVistasNoLlevanCoordenadas() {
        noFields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Vista")
                .and().areDeclaredInClassesThat().haveSimpleNameNotStartingWith("UbicacionVista")
                .should().haveNameMatching("(?i).*(latitud|longitud).*")
                .because("solo UbicacionVista, que pasa por la guardia, puede llevar coordenadas")
                .check(clases);
    }

    @Test
    void losControladoresNoAccedenALosRepositorios() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..repositorio..")
                .because("los controladores solo hablan con servicios")
                .check(clases);
    }
}