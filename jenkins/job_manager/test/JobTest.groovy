import org.junit.Test
import org.junit.BeforeClass
import groovy.mock.interceptor.*

class JobTest{
    static Repository repositoryMock
    static Logger loggerMock

    static void initializeMocks() {
        def credentialsMock = [username: " ",
                           password: " ",
                           token: " "]

        def loggerMockContext = new MockFor(Logger)
        loggerMockContext.ignore(~".*") {}
        loggerMock = loggerMockContext.proxyInstance([null])

        def repositoryMockContext = new StubFor(Repository)
        repositoryMockContext.demand.getName(0..4){"test"}
        repositoryMockContext.demand.getUrl{" "}
        repositoryMockContext.demand.getLogger{loggerMock}
        repositoryMockContext.demand.getSettings(0..4){null}
        repositoryMock = repositoryMockContext.proxyInstance([" ", " ", credentialsMock, loggerMock] as Object[])
    }

    @Test
    void testNamingConvensionVer1(){
        initializeMocks()
      def onlyTrunksMultipleSettingsYaml = '''\
        settings:
            ubuntu:
                version: trusty
            docker:
                image: shadowrobot/build-tools
                tag: trusty-indigo
            ros:
                release: indigo
            toolset:
                template_job_name: my_template
                modules:
                    - check_cache
                    - code_coverage
        trunks:
            - name: indigo-devel
            - name: kinetic-devel
              settings:
                  - ubuntu:
                        version: xenial
                    ros:
                        release: kinetic
                    docker:
                        tag: xenial-kinetic
                  - ubuntu:
                        version: willy
                    ros:
                        release: kinetic
                    docker:
                        tag: willy-kinetic'''

        def settingsExample = new SettingsParser(onlyTrunksMultipleSettingsYaml, loggerMock, "kinetic-devel")
        def branchExample = new Branch("kinetic-devel", " ", repositoryMock)

        branchExample.settings = [settingsExample.settingsList.get(0)]

        def JobNoIndexing = new Job(branchExample, settingsExample.settingsList.get(0))
        assert JobNoIndexing.name == "auto_test_kinetic-devel_kinetic"

        branchExample.settings = settingsExample.settingsList

        def JobIndex0 = new Job(branchExample, settingsExample.settingsList.get(0), 0)
        assert JobIndex0.name == "auto_test_kinetic-devel_0_kinetic"

        def JobIndex1 = new Job(branchExample, settingsExample.settingsList.get(0), 1)
        assert JobIndex1.name == "auto_test_kinetic-devel_1_kinetic"
    }

    @Test
    void testNamingConvensionVer2(){

        initializeMocks()
        def onlyTrunksMultipleSettingsYaml = '''\
        settings:
           ubuntu:
               version: trusty
           docker:
               image: shadowrobot/build-tools
               tag: trusty-indigo
           ros:
               release: indigo
           toolset:
               template_job_name: template_unit_tests_and_code_coverage
               modules:
                   - check_cache
                   - code_coverage
        trunks:
           - name: indigo-devel
           - name: kinetic-devel
             settings:
                 - ubuntu:
                       version: xenial
                   ros:
                       release: kinetic
                   docker:
                       tag: xenial-kinetic
                 - ubuntu:
                       version: willy
                   ros:
                       release: kinetic
                   docker:
                       tag: willy-kinetic
                 - ubuntu:
                       version: trusty
                   ros:
                       release: kinetic
                   docker:
                       tag: trusty-kinetic
        branch:
           parent: kinetic-devel'''

        def settingsExample = new SettingsParser(onlyTrunksMultipleSettingsYaml, loggerMock, "my_branch")
        def branchExample = new Branch("my_branch", " ", repositoryMock)

        branchExample.settings = [settingsExample.settingsList.get(0)]

        def JobNoIndexing = new Job(branchExample, settingsExample.settingsList.get(0))
        assert JobNoIndexing.name == "auto_test_my_branch_kinetic"

        branchExample.settings = settingsExample.settingsList

        def JobIndex0 = new Job(branchExample, settingsExample.settingsList.get(0), 0)
        assert JobIndex0.name == "auto_test_my_branch_0_kinetic"

        def JobIndex1 = new Job(branchExample, settingsExample.settingsList.get(0), 1)
        assert JobIndex1.name == "auto_test_my_branch_1_kinetic"

        def JobIndex2 = new Job(branchExample, settingsExample.settingsList.get(0), 2)
        assert JobIndex2.name == "auto_test_my_branch_2_kinetic"
    }

    @Test
    void testNamingConvensionVer3(){
        initializeMocks()
        def MultipleBranchSettingsYaml = '''\
        settings:
            ubuntu:
                version: trusty
            docker:
                image: shadowrobot/build-tools
                tag: trusty-indigo
            ros:
                release: indigo
            toolset:
                template_job_name: my_template
                modules:
                    - check_cache
                    - code_coverage
        trunks:
            - name: indigo-devel
              settings:
                  - ubuntu:
                        version: trusty
                    ros:
                        release: indigo
                    docker:
                        tag: trusty-indigo
                  - ubuntu:
                        version: willy
                    ros:
                        release: kinetic
                    docker:
                        tag: willy-kinetic
            - name: kinetic-devel
              settings:
                  ubuntu:
                      version: xenial
                  ros:
                      release: kinetic
                  docker:
                      tag: xenial-kinetic
        branch:
           parent: indigo-devel'''

        def settingsExample = new SettingsParser(MultipleBranchSettingsYaml, loggerMock, "my_branch")
        def branchExample = new Branch("my_branch", " ", repositoryMock)

        branchExample.settings = [settingsExample.settingsList.get(0)]

        def JobNoIndexing = new Job(branchExample, settingsExample.settingsList.get(0))
        assert JobNoIndexing.name == "auto_test_my_branch_indigo"

        branchExample.settings = settingsExample.settingsList

        def JobIndex0 = new Job(branchExample, settingsExample.settingsList.get(0), 0)
        assert JobIndex0.name == "auto_test_my_branch_0_indigo"

        def JobIndex1 = new Job(branchExample, settingsExample.settingsList.get(0), 1)
        assert JobIndex1.name == "auto_test_my_branch_1_kinetic"
    }
}