# Generate Advanced Installer Updates.

A little command to help our build pipeline. Generates Advanced Installer update descriptors
without using Advanced Installer GUI. 

Takes some simple parameters to generate the INI style file which may be uploaded to an 
update server.

Intended to be natively compiled and copied to Windows build nodes PATH. 

Use a Graal VM, run ..

```
mvn package -P native-image
```

The result will be `target/genaiu`, run with `--help` for options.

## Usage

```
Usage: genaiu [-f=FLAGS] [-n=NAME] [-o=KEY] [-pv=VERSION] -r=KEY
              [-u=URL_FOLDER] [-U=URL] -v=VERSION <inputs>...
Generate Advanced Installer Updates.
      <inputs>...          The updated installer(s). Each one should be a path
                             (relative or absolute) to an updateable installer
                             for this project. You may prefix either one with
                             '<SECTION>:', where SECTION is the name to use for
                             the section in the output file for this PATH'
  -f, --flags=FLAGS        Other flags.
  -n, --name=NAME          The name of the application. If not provided,
                             attempts will be made to generate from the
                             filename.
  -o, --output=KEY         The path to store the generated output. If ommitted,
                             printed to system output.
      -pv, --product-version=VERSION
                           The product version number. If not supplied, VERSION.
  -r, --registry-key=KEY   The registry key where version numbers are kept for
                             this application.
  -u, --base-url=URL_FOLDER
                           The folder on update server where the updated
                             installer is. The input filename will be appended
                             to this. Either this or FULL_URL must be provided.
  -U, --url=URL            The full URL on the update server where the updated
                             installer is. Either this or FULL_URL must be
                             provided.
  -v, --version=VERSION    The specific version number. Required.
```
