FactoryPick GitHub 협업 사용법
========================================

저장소 주소
https://github.com/BeliefYee/FactoryPick.git

이 문서는 팀원이 GitHub에서 프로젝트를 처음 내려받는 방법부터
브랜치에서 작업하고 Pull Request를 요청하는 과정까지 설명합니다.


1. 협업 전 기본 원칙
----------------------------------------

1) main 브랜치에서 직접 작업하지 않습니다.
2) 작업을 시작할 때마다 main 브랜치를 최신 상태로 업데이트합니다.
3) 기능별로 새로운 브랜치를 만들어 작업합니다.
4) 작업한 브랜치를 GitHub에 push한 뒤 Pull Request를 생성합니다.
5) 팀장의 승인과 병합이 끝난 후 다시 최신 main 브랜치를 내려받습니다.
6) 강제 push 명령인 git push --force는 사용하지 않습니다.


2. 최초 한 번만 설정하기
----------------------------------------

Git Bash를 열고 본인의 Git 사용자 정보를 설정합니다.

git config --global user.name "GitHub 사용자명"
git config --global user.email "GitHub 가입 이메일"

설정 확인:

git config --global user.name
git config --global user.email


3. GitHub에서 프로젝트 처음 가져오기(Clone)
----------------------------------------

먼저 팀장이 보낸 GitHub 협업 초대를 수락합니다.

Git Bash에서 프로젝트를 저장할 위치로 이동합니다.

cd ~/Desktop

저장소를 복제합니다.

git clone https://github.com/BeliefYee/FactoryPick.git

복제된 프로젝트 폴더로 이동합니다.

cd FactoryPick

연결된 원격 저장소를 확인합니다.

git remote -v

다음과 같은 주소가 나오면 정상입니다.

origin  https://github.com/BeliefYee/FactoryPick.git (fetch)
origin  https://github.com/BeliefYee/FactoryPick.git (push)


4. 작업을 시작하기 전에 main 최신화하기
----------------------------------------

main 브랜치로 이동합니다.

git switch main

GitHub의 최신 내용을 가져옵니다.

git pull origin main

반드시 이 과정을 마친 후 새로운 작업 브랜치를 만듭니다.


5. 기능 브랜치 만들기
----------------------------------------

브랜치 이름은 담당 기능을 알 수 있도록 작성합니다.

예시:

feature/frontend-map
feature/backend-search
feature/admin-page
feature/factory-detail
fix/login-error

새로운 브랜치를 만들면서 이동하는 명령:

git switch -c feature/frontend-map

현재 브랜치 확인:

git branch

브랜치 이름 앞에 * 표시가 있으면 현재 작업 중인 브랜치입니다.


6. 파일 작업 후 변경사항 확인하기
----------------------------------------

코드 작성 또는 수정이 끝나면 변경된 파일을 확인합니다.

git status

주의:
다음과 같은 자동 생성 파일이나 보안 파일은 GitHub에 올리지 않습니다.

backend/.gradle
backend/build
frontend/node_modules
frontend/dist
.env

node_modules처럼 수천 개의 파일이 표시되면 commit하지 말고
.gitignore 설정을 먼저 확인합니다.


7. 변경사항 Commit하기
----------------------------------------

모든 변경사항을 스테이징합니다.

git add .

다시 상태를 확인합니다.

git status

커밋 메시지와 함께 저장합니다.

git commit -m "feat: 지도 화면 구현"

커밋 메시지 작성 예시:

feat: 새로운 기능 추가
fix: 오류 수정
docs: 문서 수정
style: 화면 스타일 수정
refactor: 코드 구조 개선
test: 테스트 코드 추가
chore: 설정 및 기타 작업


8. 작업 브랜치를 GitHub에 Push하기
----------------------------------------

처음 push하는 브랜치는 다음 명령을 사용합니다.

git push -u origin feature/frontend-map

feature/frontend-map 부분은 본인이 만든 브랜치 이름으로 변경합니다.

같은 브랜치에 추가 작업을 commit한 뒤 다시 올릴 때는 다음 명령만 사용해도 됩니다.

git push

main 브랜치에는 직접 push하지 않습니다.


9. Pull Request 만들기
----------------------------------------

1) GitHub의 FactoryPick 저장소에 접속합니다.
2) Compare & pull request 버튼을 누릅니다.
3) base 브랜치와 compare 브랜치를 확인합니다.

base: main
compare: 본인의 작업 브랜치

예시:

base: main
compare: feature/frontend-map

4) 제목에 구현한 기능을 작성합니다.
5) 설명에 변경된 내용과 확인이 필요한 부분을 작성합니다.
6) Create pull request 버튼을 누릅니다.
7) 팀장에게 검토를 요청합니다.

Pull Request 작성 예시:

제목: 지도 메인 화면 구현

내용:
- 대한민국 지도 화면 추가
- 백엔드 공장 마커 API 연결
- 카테고리별 아이콘 표시
- 실행 및 화면 표시 확인 완료


10. Pull Request 승인 및 병합 후 최신 내용 받기
----------------------------------------

팀장이 Pull Request를 승인하고 main에 병합하면 로컬 프로젝트도 업데이트합니다.

git switch main
git pull origin main

병합이 완료된 로컬 작업 브랜치는 삭제할 수 있습니다.

git branch -d feature/frontend-map

GitHub의 원격 브랜치까지 삭제된 경우 다음 명령으로 정리할 수 있습니다.

git fetch --prune


11. 다른 작업을 시작할 때 반복하는 순서
----------------------------------------

git switch main
git pull origin main
git switch -c feature/새로운기능

파일 작업 후:

git status
git add .
git commit -m "feat: 새로운 기능 구현"
git push -u origin feature/새로운기능

마지막으로 GitHub에서 Pull Request를 생성합니다.


12. 작업 중 main 변경사항을 내 브랜치에 반영하기
----------------------------------------

다른 팀원의 코드가 main에 먼저 병합되었다면 다음 순서로 가져옵니다.

현재 변경사항을 먼저 commit한 후 실행합니다.

git switch main
git pull origin main
git switch feature/본인브랜치
git merge main

충돌이 없으면 계속 작업하고 push합니다.

git push


13. 충돌이 발생했을 때
----------------------------------------

충돌이 발생하면 먼저 충돌 파일을 확인합니다.

git status

충돌 파일 안에는 다음과 같은 표시가 나타납니다.

<<<<<<< HEAD
내 브랜치의 코드
=======
main 브랜치의 코드
>>>>>>> main

필요한 코드를 남기고 위의 충돌 표시를 모두 삭제합니다.
수정한 파일을 저장한 다음 실행합니다.

git add .
git commit -m "merge: main 브랜치 충돌 해결"
git push

어떤 코드를 남겨야 할지 확실하지 않으면 임의로 삭제하지 말고
해당 코드를 작성한 팀원과 확인한 후 처리합니다.


14. 자주 사용하는 확인 명령
----------------------------------------

현재 상태 확인:
git status

현재 브랜치 확인:
git branch

원격 저장소 확인:
git remote -v

커밋 기록 확인:
git log --oneline --graph --all

GitHub의 최신 브랜치 정보 가져오기:
git fetch origin


15. 사용하면 안 되는 명령
----------------------------------------

팀원은 다음 명령을 임의로 사용하지 않습니다.

git push --force
git reset --hard
git clean -fd
git branch -D 브랜치명

위 명령들은 커밋이나 작업 파일을 강제로 덮어쓰거나 삭제할 수 있습니다.
필요한 상황이라면 반드시 팀장과 상의한 후 사용합니다.


16. 전체 작업 흐름 요약
----------------------------------------

최초 1회:

1) GitHub 초대 수락
2) git clone으로 프로젝트 복제
3) cd FactoryPick으로 이동

매 작업 시:

1) main 브랜치로 이동
2) 최신 main pull
3) 새로운 기능 브랜치 생성
4) 코드 작성
5) git add 및 commit
6) 기능 브랜치 push
7) Pull Request 생성
8) 팀장 검토 및 승인
9) main 병합
10) 로컬 main 최신화

