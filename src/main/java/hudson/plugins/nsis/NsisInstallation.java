package hudson.plugins.nsis;

import hudson.EnvVars;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;

/**
 * @author Gregory Boissinot
 */
public final class NsisInstallation extends ToolInstallation implements NodeSpecific<NsisInstallation>, EnvironmentSpecific<NsisInstallation> {

    @DataBoundConstructor
    public NsisInstallation(String name, String home) {
        super(name, home, null);
    }

    public NsisInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new NsisInstallation(getName(), translateFor(node, log));
    }

    public NsisInstallation forEnvironment(EnvVars environment) {
        return new NsisInstallation(getName(), environment.expand(getHome()));
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NsisInstallation> {

        public String getDisplayName() {
            return "Nsis Build";
        }

        public FormValidation doCheckName(@QueryParameter String value) throws IOException {
            if(value == null || value.length()==0)
                return FormValidation.error("Please set a name");
            return FormValidation.ok();
        }

        public FormValidation doCheckHome(@QueryParameter File value) throws IOException {
        	if(!Hudson.getInstance().hasPermission(Hudson.ADMINISTER))
                return FormValidation.ok();

            if(value == null || value.getPath().length()==0)
                return FormValidation.error("Please set a path makensis.exe");
            return FormValidation.ok();
        }

        @Override
        public NsisInstallation[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(NsisBuilder.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(NsisInstallation... installations) {
            Hudson.getInstance().getDescriptorByType(NsisBuilder.DescriptorImpl.class).setInstallations(installations);
        }

    }

}
