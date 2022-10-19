# Relief-Post-Office
![image](https://user-images.githubusercontent.com/78855847/189635986-04fd87b5-38e7-4628-9880-6c07c25087a4.png)

## 앱 설치하기
안드로이드 폰에 아래 apk파일 설치 후 실행  
https://github.com/Relief-Post-Office/Relief-Post-Office/blob/main/app-release2.apk?raw=true 

## 프로젝트 목적
**사회적 취약 계층**과 복지 종사자 및 지역 사회의 일원들을 연결하여  
취약 계층에게 **정서적인 지원**과 **지속적인 관리**를 제공할 수 있는 새로운 솔루션을 개발하기 위함

## 주요 기능
### 보호자 로그인 시
<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/guardian_main.png?raw=true" width="500" > </p>
<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/guardian_question.png?raw=true" width="500" > <img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/guardian_safety.png?raw=true" width="500" ></p>

<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/ward_profile.png?raw=true" width="500" > <img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/ward_safety.png?raw=true" width="500" ></p>

<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/ward_result.png?raw=true" width="500" > <img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/ward_result_detail.png?raw=true" width="500" ></p>

### 피보호자 로그인 시
<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/ward_main.png?raw=true" width="500" > <img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/images/ward_answer.png?raw=true" width="500" ></p>

## 개발 언어&환경
### 개발 언어
- Kotlin

### 개발 환경
- Android Studio Chipmunk
- Firebase
  - Realtime Database
  - Storage
  - Authentication
  - Cloud Messaging
  - In-App Messaging
  - Hosting
  
## SDK 버전
- minSdkVersion : 23
- targetSdkVersion : 32

## 소스 디렉토리 구조
![image](https://user-images.githubusercontent.com/78855847/190316290-b8c222a3-cdec-4d79-84da-7f801ebca0e0.png)

## 프로젝트 아키텍처
<p align="center"><img src="https://user-images.githubusercontent.com/81675254/190188415-25540ffd-0b89-4678-b8b8-c846392b6776.png" width="1000" > </p>

본 프로젝트는 Firebase에서 제공하는 기능들을 활용하여 **Serverless 아키텍쳐**로 구축됨<br/> 
서버가 없는 아키텍쳐에서 보호자-피보호자간의 **동기화**를 수행하기 위하여 **"자동 알람 시스템"** 구축

## 자동 알람 시스템
<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/diagram/Alarm.png?raw=true" width="500" > </p>
<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/diagram/WardAlarm.png?raw=true" width="500" > <img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/diagram/GuardianAlarm.png?raw=true" width="500" ></p>
<p align="center"><img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/diagram/BootAlarm.png?raw=true" width="500" > <img src="https://github.com/Relief-Post-Office/Relief-Post-Office/blob/docs/diagram/NetworkAlarm.png?raw=true" width="500" ></p>

## Credit
**신동해** : github.com/Jeensh  
**장승범** : github.com/seungbeom1010  
**조명재** : github.com/moungJae  
**채문희** : github.com/moonheee  

## 이미지 출처
<a href="https://www.flaticon.com/kr/free-icons/" title="새 아이콘">새 아이콘  제작자: Freepik - Flaticon</a>  
<a href="https://www.flaticon.com/kr/free-icons/-" title="우편 집배원 아이콘">우편 집배원 아이콘  제작자: Freepik - Flaticon</a>   
<a href="https://www.flaticon.com/kr/free-icons/" title="우편 아이콘">우편 아이콘  제작자: Freepik - Flaticon</a>  

## License
This project is licensed under the GPL License - see the [LICENSE](https://github.com/Relief-Post-Office/Relief-Post-Office/blob/license/License) file for details
