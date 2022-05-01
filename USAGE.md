# Usage

* Test changes: `ansible-playbook .github/ansible/ci.yml -e "pipeline__test=true"`
* Publish changes: `ansible-playbook .github/ansible/ci.yml -e "pipeline__test=false"`
* Update table: `./mvnw clean compile; ./mvnw exec:java -Dexec.mainClass="berlin.yuna.repos.Main"; git commit -am "chore: updated readme"; git push origin head`
