기능 추가 팀을 활성화합니다. Planner / Backend / Frontend / Tester 4명의 teammate를 spawn하세요.

## 지시사항

아래 순서대로 진행하세요:

1. 다음 4개 파일을 Read하여 각 teammate의 역할과 기술 스택을 파악하세요.
   - `.claude/commands/planner.md` — Planner (기획자)
   - `.claude/commands/backend.md` — Backend Engineer
   - `.claude/commands/frontend.md` — Frontend Engineer
   - `.claude/commands/tester.md` — Tester (QA 엔지니어)

2. 파일 내용을 숙지한 뒤, **4명의 teammate를 spawn하는 에이전트 팀을 생성**하세요.
   - 각 teammate의 생성 프롬프트에는 해당 파일의 역할·기술 스택·원칙을 포함하세요.
   - teammate 이름: `planner`, `backend`, `frontend`, `tester`

3. 팀 생성 완료 후 사용자에게 팀 구성 현황을 보고하고 작업 요청을 기다리세요.

## 팀 운영 원칙

- 작업 요청이 들어오면 리더가 작업을 분석하여 적절한 teammate에게 할당하세요.
- teammate들이 독립적으로 작업할 수 있도록 명확한 작업 경계를 설정하세요.
- 각 teammate의 결과물을 종합하여 최종 보고하세요.
- 필요시 각 teammate 는 서로 의견을 교환하며 고도화를 시키는 것을 반복한다.

## 작업의 원칙
- 추가 기능에 대한 요청이 들어울 시 각 담당자는 정교하게 내용을 분석한 후 각 담당자들끼리 긴밀한 교류를한다.
- 기능에 대한 부분에 대해 조금이라도 궁금한 부분이 생기면 planner 와 상담하여 기획서를 고도화한다.
- 고도화된 기획서는 Tester 에 의해 e2e 테스트가 작성된다.
- 각 담당자들이 e2e 테스트를 검토하고 문제가 있으면 Tester와 교류해 테스트를 고도화한다.
- 위 일을 반복하고 test와 기획서 상세 정의가 완료되면 작업을 시작한다.
- e2e 테스트를 만족할 때까지 작업한다.
