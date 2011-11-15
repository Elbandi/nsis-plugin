package hudson.plugins.nsis;

import hudson.*;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author elso.andras@gmail.com
 * @author Andras Elso - Elbandi
 *         2011/11/15 - Initial release
 */
public class NsisBuilder extends Builder {

    /**
     * GUI fields
     */
    private final String nsisInstallationName;
    private final String nsisFile;
    private final String cmdLineArgs;

    /**
     * When this builder is created in the project configuration step,
     * the builder object will be created from the strings below.
     *
     * @param nsisInstallationName The nsis logical name
     * @param nsisFile The name/location of the nsis script file
     * @param cmdLineArgs Whitespace separated list of command line arguments
     */
    @DataBoundConstructor
    @SuppressWarnings("unused")
    public NsisBuilder(String nsisInstallationName, String nsisFile, String cmdLineArgs) {
        this.nsisInstallationName = nsisInstallationName;
        this.nsisFile = nsisFile;
        this.cmdLineArgs = cmdLineArgs;
    }

    @SuppressWarnings("unused")
    public String getCmdLineArgs() {
        return cmdLineArgs;
    }

    @SuppressWarnings("unused")
    public String getNsisFile() {
        return nsisFile;
    }

    @SuppressWarnings("unused")
    public String getNsisInstallationName() {
        return nsisInstallationName;
    }

    public NsisInstallation getNsisInstallation() {
        for (NsisInstallation i : DESCRIPTOR.getInstallations()) {
            if (nsisInstallationName != null && i.getName().equals(nsisInstallationName))
                return i;
        }
        return null;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();

        String execName = "makensis.exe";
        NsisInstallation ai = getNsisInstallation();
        if (ai == null) {
            listener.getLogger().println("Path To makensis.exe: " + execName);
            args.add(execName);
        } else {
            EnvVars env = build.getEnvironment(listener);
            ai = ai.forNode(Computer.currentComputer().getNode(), listener);
            ai = ai.forEnvironment(env);
            String pathToNsis = ai.getHome();
            FilePath exec = new FilePath(launcher.getChannel(), pathToNsis);
            try {
                if (!exec.exists()) {
                    listener.fatalError(pathToNsis + " doesn't exist");
                    return false;
                }
            } catch (IOException e) {
                listener.fatalError("Failed checking for existence of " + pathToNsis);
                return false;
            }
            listener.getLogger().println("Path To makensis.exe: " + pathToNsis);
            args.add(pathToNsis);
        }

        EnvVars env = build.getEnvironment(listener);

        String normalizedArgs = cmdLineArgs.replaceAll("[\t\r\n]+", " ");
        normalizedArgs = Util.replaceMacro(normalizedArgs, env);
        normalizedArgs = Util.replaceMacro(normalizedArgs, build.getBuildVariables());
        if (normalizedArgs.trim().length() > 0)
            args.addTokenized(normalizedArgs);

        //args.addKeyValuePairs("/D", build.getBuildVariables());

        String normalizedFile = nsisFile.replaceAll("[\t\r\n]+", " ");
        normalizedFile = Util.replaceMacro(normalizedFile, env);
        normalizedFile = Util.replaceMacro(normalizedFile, build.getBuildVariables());
        if (normalizedFile.length() > 0)
            args.add(normalizedFile);

        if (!launcher.isUnix()) {
            args.prepend("cmd.exe", "/C");
            args.add("&&", "exit", "%%ERRORLEVEL%%");
        }

        listener.getLogger().println("Executing command: " + args.toStringWithQuote());
        try {
            int r = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(build.getModuleRoot()).join();
            return r == 0;
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("command execution failed"));
            return false;
        }
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<Builder> {

        @CopyOnWrite
        private volatile NsisInstallation[] installations = new NsisInstallation[0];

        DescriptorImpl() {
            super(NsisBuilder.class);
            load();
        }

        public FormValidation doCheckNsisFile(@QueryParameter String value) throws IOException {
            if(value == null || value.length()==0)
                return FormValidation.error("Please set a script name");
            return FormValidation.ok();
        }

        public String getDisplayName() {
            return "Build a installers using NSIS";
        }


        public NsisInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(NsisInstallation... antInstallations) {
            this.installations = antInstallations;
            save();
        }

        /**
         * Obtains the {@link NsisInstallation.DescriptorImpl} instance.
         */
        public NsisInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(NsisInstallation.DescriptorImpl.class);
        }

    }

}
