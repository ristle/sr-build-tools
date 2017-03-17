import org.junit.Test
import org.junit.BeforeClass
import groovy.mock.interceptor.MockFor

class SettingsTest {

    static Logger loggerMock

    @BeforeClass
    static void initializeMocks() {
        def loggerMockContext = new MockFor(Logger)
        loggerMockContext.ignore(~".*") {}
        loggerMock = loggerMockContext.proxyInstance([null])
    }

    void checkBasicSettings(Settings config) {
        assert "trusty" == config.settings.ubuntu.version
        assert "shadowrobot/build-tools" == config.settings.docker.image
        assert "trusty-indigo" == config.settings.docker.tag
        assert "indigo" == config.settings.ros.release
        assert "my_template" == config.settings.toolset.template_job_name
        assert 2 == config.settings.toolset.modules.size()
        assert "check_cache" in config.settings.toolset.modules
        assert "code_coverage" in config.settings.toolset.modules
    }

    void checkKineticTrunkSettings(Settings config) {
        assert "xenial" == config.settings.ubuntu.version
        assert "shadowrobot/build-tools" == config.settings.docker.image
        assert "xenial-kinetic" == config.settings.docker.tag
        assert "kinetic" == config.settings.ros.release
        assert "my_template" == config.settings.toolset.template_job_name
        assert 2 == config.settings.toolset.modules.size()
        assert "check_cache" in config.settings.toolset.modules
        assert "code_coverage" in config.settings.toolset.modules
    }

    void checkKineticTrunkWilly(Settings config) {
        assert "willy" == config.settings.ubuntu.version
        assert "shadowrobot/build-tools" == config.settings.docker.image
        assert "xenial-kinetic" == config.settings.docker.tag
        assert "kinetic" == config.settings.ros.release
        assert "my_template" == config.settings.toolset.template_job_name
        assert 2 == config.settings.toolset.modules.size()
        assert "check_cache" in config.settings.toolset.modules
        assert "code_coverage" in config.settings.toolset.modules
    }

    @Test
    void basicSettingsCheck() {
        def simpleSettingsYaml = '''\
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
                    - code_coverage'''

        def SettingsParserDefault = new SettingsParser(simpleSettingsYaml)
        def configDefault = SettingsParserDefault.settingsList.get(0)
        checkBasicSettings(configDefault)

        def SettingsParserBranch = new SettingsParser(simpleSettingsYaml, "my_super_feature")
        def configForBranch = SettingsParserBranch.settingsList.get(0)
        checkBasicSettings(configForBranch)

        def SettingsParserTrunk = new SettingsParser(simpleSettingsYaml, "kinetic-devel")
        def configForTrunk = SettingsParserTrunk.settingsList.get(0)
        checkBasicSettings(configForTrunk)
    }

    @Test
    void onlyTrunksConfiguration() {
        def onlyTrunksSettingsYaml = '''\
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
                  ubuntu:
                      version: xenial
                  ros:
                      release: kinetic
                  docker:
                      tag: xenial-kinetic'''

        def SettingsParserDefault = new SettingsParser(onlyTrunksSettingsYaml)
        def configDefault = SettingsParserDefault.settingsList.get(0)
        checkBasicSettings(configDefault)

        def SettingsParserBranch = new SettingsParser(onlyTrunksSettingsYaml, "my_super_feature")
        def configForBranch = SettingsParserBranch.settingsList.get(0)
        checkBasicSettings(configForBranch)

        def SettingsParserIndigoTrunk = new SettingsParser(onlyTrunksSettingsYaml, "indigo-devel")
        def configForIndigoTrunk = SettingsParserIndigoTrunk.settingsList.get(0)
        checkBasicSettings(configForIndigoTrunk)

        def SettingsParserKineticTrunk = new SettingsParser(onlyTrunksSettingsYaml, "kinetic-devel")
        def configForKineticTrunk = SettingsParserKineticTrunk.settingsList.get(0)
        checkKineticTrunkSettings(configForKineticTrunk)
    }

    @Test
    void checkBranchInheritedSettings() {
        def branchInheritedSettingsYaml = '''\
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
                  ubuntu:
                      version: xenial
                  ros:
                      release: kinetic
                  docker:
                      tag: xenial-kinetic
        branch:
            parent: kinetic-devel'''

        def SettingsParserDefault = new SettingsParser(branchInheritedSettingsYaml)
        def configDefault = SettingsParserDefault.settingsList.get(0)
        checkBasicSettings(configDefault)

        def SettingsParserBranch = new SettingsParser(branchInheritedSettingsYaml, "my_kinetic_branch")
        def configForBranch = SettingsParserBranch.settingsList.get(0)
        checkKineticTrunkSettings(configForBranch)

        def SettingsParserIndigoTrunk = new SettingsParser(branchInheritedSettingsYaml, "indigo-devel")
        def configForIndigoTrunk = SettingsParserIndigoTrunk.settingsList.get(0)
        checkBasicSettings(configForIndigoTrunk)

        def SettingsParserKineticTrunk = new SettingsParser(branchInheritedSettingsYaml, "kinetic-devel")
        def configForKineticTrunk = SettingsParserKineticTrunk.settingsList.get(0)
        checkKineticTrunkSettings(configForKineticTrunk)
    }

    @Test
    void onlyTrunksMultipleSettings() {
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
                        tag: xenial-kinetic'''

        def SettingsParserDefault = new SettingsParser(onlyTrunksMultipleSettingsYaml)
        def configDefault = SettingsParserDefault.settingsList.get(0)
        checkBasicSettings(configDefault)

        def SettingsParserBranch = new SettingsParser(onlyTrunksMultipleSettingsYaml, "my_super_feature")
        def configForBranch = SettingsParserBranch.settingsList.get(0)
        checkBasicSettings(configForBranch)

        def SettingsParserIndigoTrunk = new SettingsParser(onlyTrunksMultipleSettingsYaml, "indigo-devel")
        def configForIndigoTrunk = SettingsParserIndigoTrunk.settingsList.get(0)
        checkBasicSettings(configForIndigoTrunk)

        def SettingsParserKineticTrunk = new SettingsParser(onlyTrunksMultipleSettingsYaml, "kinetic-devel")
        assert 2 == SettingsParserKineticTrunk.settingsList.size()
        checkKineticTrunkSettings(SettingsParserKineticTrunk.settingsList.get(0))
        checkKineticTrunkWilly(SettingsParserKineticTrunk.settingsList.get(1))

    }

    @Test
    void checkBranchInheritedMultipleSettings() {
        def branchInheritedMultipleSettingsYaml = '''\
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
                        tag: xenial-kinetic
        branch:
            parent: kinetic-devel'''

        def SettingsParserDefault = new SettingsParser(branchInheritedMultipleSettingsYaml)
        def configDefault = SettingsParserDefault.settingsList.get(0)
        checkBasicSettings(configDefault)

        def SettingsParserBranch = new SettingsParser(branchInheritedMultipleSettingsYaml, "my_kinetic_branch")
        def configForBranch = SettingsParserBranch.settingsList.get(0)
        checkKineticTrunkSettings(configForBranch)

        def SettingsParserIndigoTrunk = new SettingsParser(branchInheritedMultipleSettingsYaml, "indigo-devel")
        def configForIndigoTrunk = SettingsParserIndigoTrunk.settingsList.get(0)
        checkBasicSettings(configForIndigoTrunk)

        def SettingsParserKineticTrunk = new SettingsParser(branchInheritedMultipleSettingsYaml, "kinetic-devel")
        assert 2 == SettingsParserKineticTrunk.settingsList.size()
        checkKineticTrunkSettings(SettingsParserKineticTrunk.settingsList.get(0))
        checkKineticTrunkWilly(SettingsParserKineticTrunk.settingsList.get(1))

    }

    @Test
    void checkModulesListOverride() {
        def branchOverridesModulesListSettingsYaml = '''\
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
                  ubuntu:
                      version: xenial
                  ros:
                      release: kinetic
                  docker:
                      tag: xenial-kinetic
                  toolset:
                      modules:
                          - check_cache
                          - all_tests
        branch:
            parent: kinetic-devel
            settings:
                toolset:
                    modules:
                        - check_cache
                        - software_tests'''

        def SettingsParserDefault = new SettingsParser(branchOverridesModulesListSettingsYaml)
        def configDefault = SettingsParserDefault.settingsList.get(0)
  //      def configDefault = new Settings(branchOverridesModulesListSettingsYaml, loggerMock)
        checkBasicSettings(configDefault)

        def SettingsForBranch = new SettingsParser(branchOverridesModulesListSettingsYaml, "my_new_kinetic_branch")
        def configForBranch = SettingsForBranch.settingsList.get(0)

        assert "xenial" == configForBranch.settings.ubuntu.version
        assert "xenial-kinetic" == configForBranch.settings.docker.tag
        assert "kinetic" == configForBranch.settings.ros.release
        assert 2 == configForBranch.settings.toolset.modules.size()
        assert "check_cache" in configForBranch.settings.toolset.modules
        assert "software_tests" in configForBranch.settings.toolset.modules

        def SettingsForIndigoTrunk = new SettingsParser(branchOverridesModulesListSettingsYaml, "indigo-devel")
        def configForIndigoTrunk = SettingsForIndigoTrunk.settingsList.get(0)
        checkBasicSettings(configForIndigoTrunk)

        def SettingsForKineticTrunk = new SettingsParser(branchOverridesModulesListSettingsYaml, "kinetic-devel")
        def configForKineticTrunk = SettingsForKineticTrunk.settingsList.get(0)

        assert "xenial" == configForKineticTrunk.settings.ubuntu.version
        assert "xenial-kinetic" == configForKineticTrunk.settings.docker.tag
        assert "kinetic" == configForKineticTrunk.settings.ros.release
        assert 2 == configForKineticTrunk.settings.toolset.modules.size()
        assert "check_cache" in configForKineticTrunk.settings.toolset.modules
        assert "all_tests" in configForKineticTrunk.settings.toolset.modules


    }


}
