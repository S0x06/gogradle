/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.blindpirate.gogradle

import com.github.blindpirate.gogradle.support.IntegrationTestSupport
import com.github.blindpirate.gogradle.support.WithGitRepos
import com.github.blindpirate.gogradle.support.WithIsolatedUserhome
import com.github.blindpirate.gogradle.support.WithMockGo
import com.github.blindpirate.gogradle.support.WithResource
import com.github.blindpirate.gogradle.util.IOUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GogradleRunner)
@WithGitRepos(repoNames = ['a', 'b', 'c'], fileNames = ['a.go', 'b.go', 'c.go'])
@WithResource('')
@WithMockGo
@WithIsolatedUserhome
class IncrementalInstallationTest extends IntegrationTestSupport {
    File resource

    @Before
    void setUp() {
        writeBuildAndSettingsDotGradle("""
${buildDotGradleBase}
golang {
    packagePath='github.com/my/project'
}

repositories {
    golang {
        root ~/localhost.\\w/
        url {it.replace('localhost','http://localhost:8080')}
    }
}

dependencies {
    golang {
        build 'localhost/a'
        build 'localhost/b'
    }
}
""")
    }

    @Test
    void 'incremental installation should succeed'() {
        newBuild {
            it.forTasks('installBuildDependencies')
        }

        assert new File(resource, '.gogradle/build_gopath/src/localhost/a/.CURRENT_VERSION').exists()
        assert new File(resource, '.gogradle/build_gopath/src/localhost/b/.CURRENT_VERSION').exists()
        assert !new File(resource, '.gogradle/build_gopath/src/localhost/c/.CURRENT_VERSION').exists()

        IOUtils.write(resource, '.gogradle/build_gopath/src/localhost/b/IT_WILL_STAY', '')
        writeBuildAndSettingsDotGradle("""
${buildDotGradleBase}
golang {
    packagePath='github.com/my/project'
}

repositories {
    golang {
        root ~/localhost.\\w/
        url {it.replace('localhost','http://localhost:8080')}
    }
}

dependencies {
    golang {
        build 'localhost/b'
        build 'localhost/c'
    }
}
""")

        newBuild {
            it.forTasks('installBuildDependencies')
        }

        assert !new File(resource, '.gogradle/build_gopath/src/localhost/a/.CURRENT_VERSION').exists()
        assert new File(resource, '.gogradle/build_gopath/src/localhost/b/.CURRENT_VERSION').exists()
        assert new File(resource, '.gogradle/build_gopath/src/localhost/c/.CURRENT_VERSION').exists()
        assert new File(resource, '.gogradle/build_gopath/src/localhost/b/IT_WILL_STAY').exists()
    }

    @Override
    File getProjectRoot() {
        return resource
    }
}