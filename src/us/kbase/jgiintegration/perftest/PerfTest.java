package us.kbase.jgiintegration.perftest;

import static us.kbase.jgiintegration.common.JGIUtils.wipeRemoteServer;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.common.test.TestException;
import us.kbase.jgiintegration.common.JGIFileLocation;
import us.kbase.jgiintegration.common.JGIOrganismPage;
import us.kbase.jgiintegration.common.JGIOrganismPage.JGIPermissionsException;

import com.gargoylesoftware.htmlunit.WebClient;


public class PerfTest {
	
	private static final boolean SKIP_PUSH = true; //for testing
	private static final int NUM_FILES_TO_PUSH = 10; //200;
	private static final boolean SKIP_WIPE = true;
	
	private static final String JGI_PUSHABLE_FILE =
			"/home/crusherofheads/localgit/jgi_kbase_integration_tests/test_data/putative_pushable_organisms";
	
	private static final String FILE_ERROR_MARKER = "***ERROR***";
	
	private static final String QC = "QC Filtered Raw Data";
	private static final String RAW = "Raw Data";
	
	private static final String WIPE_URL = 
			"http://dev03.berkeley.kbase.us:9000";

	private static String JGI_USER;
	private static String JGI_PWD;
	private static String KB_USER_1;
	private static String KB_PWD_1;

	@BeforeClass
	public static void setUpClass() throws Exception {
		Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		JGI_USER = System.getProperty("test.jgi.user");
		JGI_PWD = System.getProperty("test.jgi.pwd");
		KB_USER_1 = System.getProperty("test.kbase.user1");
		KB_PWD_1 = System.getProperty("test.kbase.pwd1");
		
		String wipeUser = System.getProperty("test.kbase.wipe_user");
		String wipePwd = System.getProperty("test.kbase.wipe_pwd");
		if (!SKIP_WIPE) {
			wipeRemoteServer(new URL(WIPE_URL), wipeUser, wipePwd);
		}
	}
	
	@Test
	public void dumpJobsIntoPtKBQueue() throws Exception {
		//log in once with a known good page and then keep the same client
		WebClient cli = new WebClient();
		new JGIOrganismPage(cli, "BlaspURHD0036", JGI_USER, JGI_PWD);
		
		List<String> lines = Files.readAllLines(
				new File(JGI_PUSHABLE_FILE).toPath(),
					Charset.forName("UTF-8"));
		Collections.reverse(lines); //start with newer projects, fewer 404s, less chance of file on tape
		List<PushedFile> pushed = new LinkedList<PerfTest.PushedFile>();
		for (String line: lines) {
			if (!line.contains(FILE_ERROR_MARKER)) {
				String[] split = line.split("\t");
				String[] organisms = split[1].split(",");
				if (organisms.length < 1) {
					throw new TestException(
							"Invalid file line: no organism:\n" + line);
				}
				push(cli, pushed, organisms[0]); //just do the first org
			}
			if (pushed.size() >= NUM_FILES_TO_PUSH) {
				break;
			}
		}
		for (PushedFile file: pushed) {
			System.out.println(file.getWorkspace() + "\t" + file.getFile());
		}
	}
	
	private void push(WebClient cli, List<PushedFile> pushed, String organism)
			throws Exception {
		JGIOrganismPage org;
		try {
//			org = new JGIOrganismPage(cli, "BlaspURHD0036", JGI_USER, JGI_PWD);
			org = new JGIOrganismPage(cli, organism, null, null);
		} catch (JGIPermissionsException e) {
			System.out.println("No permissions for page " + organism);
			return;
		}
		List<String> fileGroups = org.listFileGroups();
		System.out.println(fileGroups);
		if (fileGroups.contains(QC)) {
			pushed.addAll(pushFileGroup(org, QC));
			
		}
		if (fileGroups.contains(RAW)) {
			pushed.addAll(pushFileGroup(org, RAW));
		}
	}

	private List<PushedFile> pushFileGroup(
			JGIOrganismPage org,
			String fileGroup)
			throws Exception {
		String workspace = org.getWorkspaceName("foo");
		System.out.println(fileGroup);
		System.out.println(org.listFiles(fileGroup));
		List<PushedFile> ret = new LinkedList<PerfTest.PushedFile>();
		for (String file: org.listFiles(fileGroup)) {
			if (!SKIP_PUSH) {
				org.selectFile(new JGIFileLocation(fileGroup, file));
				org.pushToKBase(KB_USER_1, KB_PWD_1);
			}
			ret.add(new PushedFile(workspace, file));
		}
		return ret;
	}
	
	private static class PushedFile {
		
		private final String workspace;
		private final String file;
		
		public PushedFile(String workspace, String file) {
			super();
			this.workspace = workspace;
			this.file = file;
		}
		
		public String getWorkspace() {
			return workspace;
		}

		public String getFile() {
			return file;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("PushedFile [workspace=");
			builder.append(workspace);
			builder.append(", file=");
			builder.append(file);
			builder.append("]");
			return builder.toString();
		}
	}

}
