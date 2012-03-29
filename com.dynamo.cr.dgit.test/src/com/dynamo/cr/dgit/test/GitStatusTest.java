package com.dynamo.cr.dgit.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jgit.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.dynamo.server.dgit.CommandUtil;
import com.dynamo.server.dgit.GitFactory;
import com.dynamo.server.dgit.GitFactory.Type;
import com.dynamo.server.dgit.GitState;
import com.dynamo.server.dgit.GitStatus;
import com.dynamo.server.dgit.IGit;

@RunWith(value = Parameterized.class)
public class GitStatusTest {

    private Type type;
    private IGit git;
    private String repoA;

    public static final String MASTER = "tmp/source_repo";

    public GitStatusTest(Type type) {
        this.type = type;
    }

    @Parameters
    public static Collection<Type[]> data() {
        Type[][] data = new Type[][] { { Type.JGIT }, { Type.CGIT } };
        return Arrays.asList(data);
    }

    private void execCommand(String command) throws IOException {
        CommandUtil.Result r = CommandUtil.execCommand(new String[] {"/bin/bash", command});
        if (r.exitValue != 0) {
            System.err.println(r.stdOut);
            System.err.println(r.stdErr);
        }
        assertEquals(0, r.exitValue);
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        execCommand("scripts/setup_empty_testgit_repo.sh");
        this.git = GitFactory.create(this.type);
        this.repoA = cloneRepo("a");
    }

    private String cloneRepo(String name) throws IOException {
        File f = new File("tmp", name + "_repo");
        if (f.exists()) {
            FileUtils.delete(f);
        }
        String repo = f.getPath();
        this.git.cloneRepo(MASTER, repo);
        return repo;
    }

    private void writeFile(String repo, String name, String content) throws IOException {
        File f = new File(repo, name);
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.close();
    }

    private void assertStatus(GitStatus status, String file, char indexStatus, char workingTreeStatus) {
        GitStatus.Entry entry = null;
        for (GitStatus.Entry e : status.files) {
            if (e.file.equals(file)) {
                entry = e;
                break;
            }
        }
        assertTrue(entry != null);
        assertThat(entry.workingTreeStatus, is(workingTreeStatus));
        assertThat(entry.indexStatus, is(indexStatus));
    }

    @Test
    public void testAddedUnstaged() throws Exception {
        String file = "test1";
        writeFile(this.repoA, file, "A");
        GitState state = this.git.getState(this.repoA);
        assertThat(state, is(GitState.DIRTY));
        GitStatus status = this.git.getStatus(this.repoA);
        assertStatus(status, file, '?', '?');
    }

    @Test
    public void testAddedStaged() throws Exception {
        String file = "test1";
        writeFile(this.repoA, file, "A");
        this.git.add(this.repoA, file);
        GitState state = this.git.getState(this.repoA);
        assertThat(state, is(GitState.DIRTY));
        GitStatus status = this.git.getStatus(this.repoA);
        assertThat(status.commitsAhead, is(0));
        assertThat(status.commitsBehind, is(0));
        assertStatus(status, file, 'A', ' ');
    }

    @Test
    public void testModifiedUnstaged() throws Exception {
        String file = "test";
        writeFile(this.repoA, file, "A");
        GitState state = this.git.getState(this.repoA);
        assertThat(state, is(GitState.DIRTY));
        GitStatus status = this.git.getStatus(this.repoA);
        assertStatus(status, file, ' ', 'M');
    }

    @Test
    public void testDeletedUnstaged() throws Exception {
        String file = "test";
        File f = new File(this.repoA, file);
        f.delete();
        GitState state = this.git.getState(this.repoA);
        assertThat(state, is(GitState.DIRTY));
        GitStatus status = this.git.getStatus(this.repoA);
        assertStatus(status, file, ' ', 'D');
    }

}
