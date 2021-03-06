package us.kbase.jgiintegration.perftest;

import static us.kbase.jgiintegration.common.JGIUtils.loadPushableFiles;

import java.net.URL;
import java.util.List;

import us.kbase.jgiintegration.common.JGIOrganismPage;
import us.kbase.jgiintegration.common.PushableFile;

public class TransOrgToOrgID {
	
	private static final String JGI_PUSHABLE_FILES = 
			"/home/crusherofheads/localgit/jgi_kbase_integration_tests/test_data/putative_pushable_files";
	
	public static void main(String[] args) throws Exception {
		String org = "Desulfovibrio_ferrireducens_DSM_16995_kbasetest";
		org = org.replace("_kbasetest", "");
		
		List<PushableFile> files = loadPushableFiles(JGI_PUSHABLE_FILES);
		
		for (PushableFile f: files) {
			if (f.getWorkspace().equals(org)) {
				System.out.println(JGIOrganismPage.getURLforOrganism(
						new URL("http://genome.jgi.doe.gov"),
						f.getOrganism()));
				break;
			}
		}
	}

}
