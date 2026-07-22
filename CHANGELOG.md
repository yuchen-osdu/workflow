# Changelog

## [1.1.1](https://github.com/yuchen-osdu/workflow/compare/v1.1.0...v1.1.1) (2026-07-22)


### 🐛 Bug Fixes

* '500' server error is returned for 'Create a new workflow using standard operators of orchestrator' request ([cf1a4e7](https://github.com/yuchen-osdu/workflow/commit/cf1a4e7dfcf2085a47568f829701fa41221ae7dd))
* Adding commons-io ([745c22c](https://github.com/yuchen-osdu/workflow/commit/745c22c443a7389b838062394cd27da37dfe1b33))
* Adding commons-io ([f2cd79f](https://github.com/yuchen-osdu/workflow/commit/f2cd79fd250e2b7b4c81567165a2086be02cc836))
* Adding NOSONAR since mutable list is required by calling code ([c8dffef](https://github.com/yuchen-osdu/workflow/commit/c8dffef00ac948e55dffca1ac8c9677a6042671c))
* Adding NOSONAR since mutable list is required by calling code ([0a7b126](https://github.com/yuchen-osdu/workflow/commit/0a7b126a9002f731254ad93bb123bdceb00fae77))
* **azure:** Netty-bom before core-lib-azure (lettuce 7.5.2 NoClassDefFoundError) ([8fcab7b](https://github.com/yuchen-osdu/workflow/commit/8fcab7b8fb3bb562193a206c878c0f9b1ee0fbaf))
* **azure:** Netty-bom before core-lib-azure (lettuce 7.5.2 NoClassDefFoundError) ([907ea5a](https://github.com/yuchen-osdu/workflow/commit/907ea5ab82c5d9f83f0cbba8a41d1ba02111f0dd))
* **azure:** Upgrade core-lib-azure to 3.0.1 ([#19](https://github.com/yuchen-osdu/workflow/issues/19)) ([263dbdc](https://github.com/yuchen-osdu/workflow/commit/263dbdcfba155c0447d2e22e39964ff0a35f1e84))
* **bootstrap:** Use access_token instead of id_token; harden bootstrap script ([0cbc600](https://github.com/yuchen-osdu/workflow/commit/0cbc60015199524fa1d5da3e3ca377ef0e6ba17a))
* **bootstrap:** Use access_token instead of id_token; harden bootstrap script ([d8e248e](https://github.com/yuchen-osdu/workflow/commit/d8e248e9aff35fbce510c76f0fd808f854757729))
* Code smells ([dc0006f](https://github.com/yuchen-osdu/workflow/commit/dc0006ff95c01b4a8c513512853e252fb97726fe))
* Cve and spring boot version bump ([08e5657](https://github.com/yuchen-osdu/workflow/commit/08e56576bbacce64288dcadc24d70202cf8c4e99))
* Cve and spring boot version bump ([b99a033](https://github.com/yuchen-osdu/workflow/commit/b99a033aa349632077e0a36c56a4506d4c889481))
* **cve:** Pom cleanup + spring-boot 3.5.16 CVE remediation ([b4629f4](https://github.com/yuchen-osdu/workflow/commit/b4629f42e39929edd9932a926f6f9765a9b91a5f))
* **cve:** Pom cleanup + spring-boot 3.5.16 CVE remediation ([8fcf4ea](https://github.com/yuchen-osdu/workflow/commit/8fcf4eac1d1a22ec08131da74d204f22109fed3d))
* Moving test scripts in the buildspec commands ([279ffbe](https://github.com/yuchen-osdu/workflow/commit/279ffbe208d8821fc5ca07d2299168972abb4061))
* Netty smtp CVE ([50135c4](https://github.com/yuchen-osdu/workflow/commit/50135c4e98842016ed2a1dee52ac12a290633509))
* Netty smtp CVE ([9da27d9](https://github.com/yuchen-osdu/workflow/commit/9da27d960b65f068230f4367f1096c1bb086023e))
* Netty-codec tomcat-core cve ([965be30](https://github.com/yuchen-osdu/workflow/commit/965be300520bdb07ebaf462cd4672f654721168b))
* Netty-codec tomcat-core cve ([972e035](https://github.com/yuchen-osdu/workflow/commit/972e035a4b725dd180f6447f8d809bfd837e5780))
* Spring cves ([b69f1a8](https://github.com/yuchen-osdu/workflow/commit/b69f1a8ff70f1ffd4873cbc5e2dd2f537e3e5e4e))
* Spring cves ([b326712](https://github.com/yuchen-osdu/workflow/commit/b326712d97452577e3144733962f45d1cea1921a))
* Spring cves ([7a63826](https://github.com/yuchen-osdu/workflow/commit/7a6382692aa7bd3ee7f139bebbfb9c68324243de))
* Spring-core bump ([64abcd7](https://github.com/yuchen-osdu/workflow/commit/64abcd7363bee3710cbd1084dae6368bfd420d5a))
* Spring-core bump ([3aae5df](https://github.com/yuchen-osdu/workflow/commit/3aae5df78c35acbfaae2531707b1db8d7a7d3fb2))
* Tomcat core CVE ([19874b5](https://github.com/yuchen-osdu/workflow/commit/19874b500a8b3030ab800e49a1096d416a2db9a1))
* Tomcat core CVE ([4fcaadd](https://github.com/yuchen-osdu/workflow/commit/4fcaaddd9ea4812d622a370f75d5931830a29203))
* Tomcat-core CVE ([4c2c68e](https://github.com/yuchen-osdu/workflow/commit/4c2c68e08c4a7dcf571f65513786fa8dc7d40cb0))
* Tomcat-core CVE ([5db1cd1](https://github.com/yuchen-osdu/workflow/commit/5db1cd181edea893dcf820ea09fc61419cb815aa))
* Update replicas ([a73a2da](https://github.com/yuchen-osdu/workflow/commit/a73a2dad8dff6bc5ed413be99f011b1b6694ea6a))
* Update replicas ([094464d](https://github.com/yuchen-osdu/workflow/commit/094464d827c12b3eaed0a34b6f47da7c20d7fb84))
* Use parent.version instead of project.version ([acf6830](https://github.com/yuchen-osdu/workflow/commit/acf6830dac139a5448ce8d9209c0274b59f95c04))
* Use parent.version instead of project.version ([97d10b2](https://github.com/yuchen-osdu/workflow/commit/97d10b245759c9d8cbafbda514f723f894243b8d))


### 🔧 Miscellaneous

* **ci:** Remove IBM jobs from pipeline ([f1f19cc](https://github.com/yuchen-osdu/workflow/commit/f1f19cc9d4e03addd93617e34c0fb9b690880d66))
* **ci:** Remove IBM jobs from pipeline ([636040e](https://github.com/yuchen-osdu/workflow/commit/636040e3c89ae581c45a4964996db8be8e484160))
* Complete repository initialization ([7274738](https://github.com/yuchen-osdu/workflow/commit/727473895bf6f3a38943e7958a4b37a36a4b2ba6))
* Copy configuration and workflows from main branch ([d6aa448](https://github.com/yuchen-osdu/workflow/commit/d6aa448b231094b969072d2bf2e474aa3ec7d21f))
* Deleting aws helm chart ([a0d9931](https://github.com/yuchen-osdu/workflow/commit/a0d993135b91026a803cccd2cf7a3093ac2bdceb))
* Deleting aws helm chart ([80bac49](https://github.com/yuchen-osdu/workflow/commit/80bac49106768d37595859929578afe2d5490dd4))
* Fixing sonar issues ([b6568bc](https://github.com/yuchen-osdu/workflow/commit/b6568bcf355ad28ed03087774d7ae819cc468872))
* Fixing sonar issues ([81823c9](https://github.com/yuchen-osdu/workflow/commit/81823c90e847595757b2077e7f86222fbd05422f))
* Removing helm copy from aws buildspec ([4c169a4](https://github.com/yuchen-osdu/workflow/commit/4c169a4f56aae6fb700eedccc72f87d7b0e3c5b0))


### 🧪 Tests

* Unique workflow names in integration tests to avoid 409 Conflict on setup (AWS + CIMPL) ([de34050](https://github.com/yuchen-osdu/workflow/commit/de3405088eb22f53cf1671dab43e6bafcc1f1e58))
* Unique workflow names in integration tests to avoid 409 Conflict on setup (AWS + CIMPL) ([5549158](https://github.com/yuchen-osdu/workflow/commit/55491587deabc4bbf6b673eb80df08afccc6b894))


### ⚙️ Continuous Integration

* Add bootstrap image job ([db89c43](https://github.com/yuchen-osdu/workflow/commit/db89c431e909a081892122c8e9e0cf5cb9b39f80))
* Add variable for tests ([0ff4a53](https://github.com/yuchen-osdu/workflow/commit/0ff4a53a7f1ac62f0e3a6400a110a6ab74842e8b))
* Fix chart name ([5ac8899](https://github.com/yuchen-osdu/workflow/commit/5ac88999195ebc6e0cc1995b39884de5135f0d0b))
* Fix chart templates ([2d3f87c](https://github.com/yuchen-osdu/workflow/commit/2d3f87cdd9e23b42e667b6e198facc45222b5bb6))
* Temporary disable other providers ([5329c39](https://github.com/yuchen-osdu/workflow/commit/5329c39163a742a38ffd1cb1b441eeadd27dab5c))
