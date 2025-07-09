<!-- pom.xml -->
<dependency>
    <groupId>com.thenewentity</groupId>
    <artifactId>dropwizard-multi-config</artifactId>
    <version>0.5.25</version><!-- latest published build (Apr 2016) --> 
</dependency>

  import com.thenewentity.utils.dropwizard.MultipleConfigurationApplication;

public class CatalogApplication
        extends MultipleConfigurationApplication<CatalogConfiguration> {

    public static void main(String[] args) throws Exception {
        // hand the raw CLI args to the ctor, then run()
        new CatalogApplication(args).run();
    }

    /**
     * Pass:
     *   1) the *normal* Dropwizard command you want as default
     *   2) a “--” separator (required by the bundle)
     *   3) your default YAML
     *   4…n) optional overlay YAMLs you always want (can be empty)
     */
    CatalogApplication(String... args) {
        super(args,
              new String[] { "server", "--", "base.yml" });
              // you could add "logging-dev.yml" here for dev builds, etc.
    }

    @Override
    public void run(CatalogConfiguration cfg, Environment env) {
        // normal Dropwizard boot…
    }
}
