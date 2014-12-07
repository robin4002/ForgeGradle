package net.minecraftforge.gradle.user.patch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraftforge.gradle.delayed.DelayedObject;
import net.minecraftforge.gradle.json.forgeversion.ForgeBuild;
import net.minecraftforge.gradle.json.forgeversion.ForgeVersion;
import net.minecraftforge.gradle.user.UserExtension;

import org.gradle.api.ProjectConfigurationException;

import com.google.common.base.Strings;

public class UserPatchExtension extends UserExtension
{
    // groups:  mcVersion  forgeVersion
    //private static final Pattern VERSION_CHECK = Pattern.compile("(?:[\\w\\d.-]+):(?:[\\w\\d-]+):([\\d.]+)-([\\d.]+)-(?:[\\w\\d.]+)");
    private static final String JUST_MC = "(\\d+\\.\\d+(?:\\.\\d+)?[_pre\\d]*)";
    private static final String JUST_API = "((?:\\d+\\.){3}(\\d+))(-(?:[\\w\\.]+)?)";
    private static final Pattern API = Pattern.compile(JUST_API);
    private static final Pattern STANDARD = Pattern.compile(JUST_MC+"-"+JUST_API);
    
    private int maxFuzz = 0;
    
    protected ForgeVersion versionInfo;

    private String apiVersion;
    private ArrayList<Object> ats = new ArrayList<Object>();

    public UserPatchExtension(UserPatchBasePlugin plugin)
    {
        super(plugin);
    }
    
    public void accessT(Object obj) { at(obj); }
    public void accessTs(Object... obj) { ats(obj); }
    public void accessTransformer(Object obj) { at(obj); }
    public void accessTransformers(Object... obj) { ats(obj); }

    public void at(Object obj)
    {
        ats.add(obj);
    }

    public void ats(Object... obj)
    {
        for (Object object : obj)
            ats.add(new DelayedObject(object, project));
    }

    public List<Object> getAccessTransformers()
    {
        return ats;
    }

    public String getApiVersion()
    {
        if (apiVersion == null)
            throw new ProjectConfigurationException("You must set the Minecraft Version!", new NullPointerException());
        
        return apiVersion;
    }
    
    public int getMaxFuzz()
    {
        return maxFuzz;
    }

    public void setMaxFuzz(int fuzz)
    {
        this.maxFuzz = fuzz;
    }
    
    public void setVersion(String str) // magic goes here
    {
        str = str.trim();
        
        // build number
        if (isAllNums(str))
        {
            boolean worked = getFromBuildNumber(str);
            if (worked)
                return;
        }
        
        // promotions
        if (versionInfo.promos.containsKey(str))
        {
            boolean worked = getFromBuildNumber(versionInfo.promos.get(str));
            project.getLogger().lifecycle("Selected version " +apiVersion);
            if (worked)
                return;
        }
        
        // matches just an API version
        Matcher matcher = API.matcher(str);
        if (matcher.matches())
        {
            String branch = Strings.emptyToNull(matcher.group(3));
            
            String forgeVersion = matcher.group(1);
            ForgeBuild build = versionInfo.number.get(Integer.valueOf(matcher.group(2)));
            
            boolean branchMatches = false;
            if (branch == null)
                branchMatches = Strings.isNullOrEmpty(build.branch);
            else
                branchMatches = branch.substring(1).equals(build.branch);
            
            if (!build.version.equals(forgeVersion) || !branchMatches)
            {
                String outBranch = build.branch;
                if (outBranch == null)
                    outBranch = "";
                else
                    outBranch = "-" + build.branch;
                
                throw new RuntimeException(str+" is an invalid version! did you mean '"+build.version+outBranch+"' ?");
            }
            
            version = build.mcversion.replace("_", "-");
            apiVersion = version + "-" + build.version;
            if (!Strings.isNullOrEmpty(build.branch) && !"null".equals(build.branch))
                apiVersion += "-" + build.branch;
            
            return;
        }
        
        // matches standard form.
        matcher = STANDARD.matcher(str);
        if (matcher.matches())
        {
            String branch = Strings.emptyToNull(matcher.group(4));
            String mcversion = matcher.group(1);
            
            String forgeVersion = matcher.group(2);
            ForgeBuild build = versionInfo.number.get(Integer.valueOf(matcher.group(3)));
            
            boolean branchMatches = false;
            if (branch == null)
                branchMatches = Strings.isNullOrEmpty(build.branch);
            else
                branchMatches = branch.substring(1).equals(build.branch);
            
            boolean mcMatches = build.mcversion.equals(mcversion);
            
            if (!build.version.equals(forgeVersion) || !branchMatches || !mcMatches)
            {
                String outBranch = build.branch;
                if (outBranch == null)
                    outBranch = "";
                else
                    outBranch = "-" + build.branch;
                
                throw new RuntimeException(str+" is an invalid version! did you mean '"+build.mcversion+"-"+build.version+outBranch+"' ?");
            }
            
            version = build.mcversion.replace("_", "-");
            apiVersion = version + "-" + build.version;
            if (!Strings.isNullOrEmpty(build.branch) && !"null".equals(build.branch))
                apiVersion += "-" + build.branch;
            
            return;
        }
        
        throw new RuntimeException("Invalid version notation! The following are valid notations. Buildnumber, version, version-branch, mcversion-version-branch, and pomotion");
    }
    
    private boolean isAllNums(String in)
    {
        for (char c : in.toCharArray())
        {
            if (!Character.isDigit(c))
                return false;
        }
        
        return true;
    }
    
    private boolean getFromBuildNumber(String str)
    {
        return getFromBuildNumber(Integer.valueOf(str));
    }
    
    private boolean getFromBuildNumber(Integer num)
    {
        ForgeBuild build = versionInfo.number.get(num);
        if (build != null)
        {
            version = build.mcversion.replace("_", "-");
            apiVersion = version + "-" + build.version;
            if (!Strings.isNullOrEmpty(build.branch) && !"null".equals(build.branch))
                apiVersion += "-" + build.branch;
            
            return true;
        }
        else
            return false;
    }
}
