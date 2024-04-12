package com.logonbox.genaiu;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.UncheckedException;

import com.sshtools.jini.INI;
import com.sshtools.jini.INIWriter;
import com.sshtools.jini.INIWriter.EscapeMode;
import com.sshtools.jini.INIWriter.StringQuoteMode;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "genaiu", /* mixinStandardHelpOptions = true, */
		description = "Generate Advanced Installer Updates."/* , versionProvider = GenAIU.Version.class */)
public class GenAIU implements Callable<Integer> {

	
	public final static class Version implements IVersionProvider {

		@Override
		public String[] getVersion() throws Exception {
			return new String[] { "1.0" };
		}
		
	}
	
	@Spec
	CommandSpec spec;
	
	@Option(names = {"-pv", "--product-version"}, paramLabel = "VERSION", description = "The product version number. If not supplied, VERSION.")
	private Optional<String> productVersion;
	
	@Option(names = {"-u", "--base-url"}, paramLabel = "URL_FOLDER", description = "The folder on update server where the updated installer is. The input filename will be appended to this. Either this or FULL_URL must be provided.")
	private Optional<String> baseUrl;
	
	@Option(names = {"-U", "--url"}, paramLabel = "URL", description = "The full URL on the update server where the updated installer is. Either this or FULL_URL must be provided.")
	private Optional<String> url;
	
	@Option(names = {"-v", "--version"}, paramLabel = "VERSION", description = "The specific version number. Required.", required = true)
	private String version;
	
	@Option(names = {"-o", "--output"}, paramLabel = "PATH", description = "The path to store the generated output. If ommitted, printed to system output.")
	private Optional<Path> output;
	
    @ArgGroup(exclusive = false, multiplicity = "1..*")
    List<Updater> updaters;
	
	static class Updater {
		
		@Option(names = {"-n", "--name"}, paramLabel = "NAME", description = "The name of the application. If not provided, attempts will be made to generate from the filename.")
		private Optional<String> name;
		
		@Option(names = {"-f", "--flags"}, paramLabel = "FLAGS", description = "Other flags.")
		private Optional<String> flags;
		
		@Option(names = {"-i", "--id"}, paramLabel = "ID", description = "The ID (i.e. section name) of this updater. If not specified, generated from filename.")
		private Optional<String> id;
		
		@Option(names = {"-r", "--registry-key"}, paramLabel = "KEY", description = "The registry key where version numbers are kept for this application.", required = true)
		private String registryKey;
		
		@Parameters(arity = "1", description = "The updated installer(s). Each one should be a path (relative or absolute) to an updateable installer for this project. You may prefix either one with '<SECTION>:', where SECTION is the name to use for the section in the output file for this PATH'")
		private Path input;
	}

	public String getVersion() {
		return spec.version()[0];
	}
	
	@Override
	public Integer call() throws Exception {
		
		var ini = INI.create();
		for(var updater : updaters) {
			var input = updater.input;
			var updPath = input.toString();
			var secName = updater.name.orElseGet(() -> calcSecNameFromPath(FilenameUtils.getName(updPath)));
			var serverFilename = FilenameUtils.getName(updPath);
			var fullUrl = url.orElseGet(() -> baseUrl.map(bu -> bu + "/" + serverFilename).orElseThrow(() -> new IllegalStateException("Either a URL or URL_FOLDER must be supplied.")));
			
			var sec = ini.create(secName);
			sec.put("Name", updater.name.orElseGet(() -> toEnglish(processName(FilenameUtils.getBaseName(updPath)))));
			sec.put("ProductVersion", productVersion.orElse(version));
			sec.put("URL", fullUrl);
			var path = Paths.get(updPath);
			sec.put("Size", Files.size(path));
			try(var in = Files.newInputStream(path)) {
				sec.put("SHA256", DigestUtils.sha256Hex(in));
			}
			try(var in = Files.newInputStream(path)) {
				sec.put("MD5", DigestUtils.md5Hex(in));
			}
			sec.put("ServerFileName", serverFilename);
			updater.flags.ifPresent(flgs ->
				sec.put("Flags", flgs)
			);
			sec.put("RegistryKey", updater.registryKey);
			sec.put("Version", version);
		}

		var wtr = new INIWriter.Builder().
				withStringQuoteMode(StringQuoteMode.NEVER).
				withEscapeMode(EscapeMode.NEVER).
				build();
		output.ifPresentOrElse(p -> {
			try(var out = Files.newBufferedWriter(p)) {
				new PrintWriter(out).println(";aiu;");
				wtr.write(ini, out);
			}
			catch(IOException ioe) {
				throw new UncheckedException(ioe);
			}
		}, () -> {
			System.out.println(";aiu;");
			System.out.print(wtr.write(ini));
		});
		
		return 0;
	}
	

	private String calcSecNameFromPath(String input) {
		return processName(input).toLowerCase().replace(' ', '-');
	}

	private String processName(String input) {
		return String.join(" ", input.replaceAll("[^A-Za-z0-9\\s]+", " ").split("\\s+"));
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new GenAIU()).execute(args));
	}
	
	protected void error(String message, Object... args) {
		System.err.println(String.format("genaiu: " + message, args));
	}

    private static String toEnglish(String object) {
        return toEnglish(object, true);
    }

    private  static String toEnglish(String str, boolean name) {
        if (str == null) {
            return "";
        }
        boolean newWord = true;
        StringBuffer newStr = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            ch = Character.toLowerCase(ch);
            if (ch == '_') {
                ch = ' ';
            }
            if (ch == ' ') {
                newWord = true;
            } else if (newWord && name) {
                ch = Character.toUpperCase(ch);
                newWord = false;
            }
            newStr.append(ch);
        }
        return newStr.toString();
    }

}
