

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by seodongmin on 2019-07-15 12:37.
 * Modified by seodongmin on 2019-07-24 12:29.
 */

public class WivServer {

    private final String basicURL = "http://dak2183242.cafe24.com/wiv/";
    private HashMap<String, HashMap<String, Subject>> watchingSubjects; //교양 4개의 분류, 40여개의 학과가 키가 된다
    //교양 4분류 키의 예시: A01, 40여개의 학과 키 예시:
    private String result = "";

    //생성자
    private WivServer() {
        watchingSubjects = new HashMap<String, HashMap<String, Subject>>();
    }

    //시작함수
    private void start() {
        //시작할 때, 이미 저장되어 있던 요청사항을 모두 불러온다(서버 재시작시 필요한 기능)
        //어디서? 내 DB 에서

        while(true) {


            //watching subject 목록 불러오기
            Thread resultFetcher = new Thread(new Runnable() {
                @Override
                public void run() {
                    result = doHTMLWorks("fetch_watching_subjects.php", "secCode=onlythiswivappcancallthisfetchwatchingsubjectsphpfile!", "");
                }
            });
            resultFetcher.start();
            try {
                resultFetcher.join();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            try {
                //JSON Array 받음
                JSONArray jsonArray = new JSONObject(result).getJSONArray("watching_subjects");

                //watching 하고있는 각 과목에 대하여~
                for(int i = 0; i < jsonArray.length(); i ++) {
                    //object 추출
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    //System.out.println(jsonObject.toString());

                    //추출한 내용을 바탕으로 subject element 생성
                    Subject subject = new Subject();
                    subject.setSubjectNo(jsonObject.getString("subject_no"));
                    subject.setSubjectDiv(jsonObject.getString("subject_div"));
                    subject.setClassDiv(jsonObject.getString("class_div"));
                    subject.setDept(jsonObject.getString("dept"));
                    subject.setSubDept(jsonObject.getString("sub_dept"));
                    subject.setSubjectName(jsonObject.getString("subject_name"));

                    //subject 를 적절한 HashMap 에 Mapping
                    mappingSubject(subject);
                }

                //각 HashMap 에 대한 비교 스레드 생성
                makeWatchingSubjectThread();
            } catch(JSONException e) {  //JSONException, ArrayIndexOutOfBoundsException
                e.printStackTrace();
            }

            System.out.println("=====리스트 1회 출력 완료=====");

            //1초 후 같은 행위 반복
            try {
                Thread.sleep(500);
                System.gc();
            } catch(InterruptedException e ) {
                e.printStackTrace();
            }
        }

    } // start()

    //메인함수
    public static void main(String[] args) {
        new WivServer().start();
    }


    private static String getTagValue(String tag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(tag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);


        if(nValue == null)
            return null;
        return nValue.getNodeValue();
    }

    //메서드
    private String doHTMLWorks(String urlString, String arguments, String errorMessage) {

        String param = "";
        String result = "";
        try {
            URL url = new URL(basicURL + urlString + "?" + arguments);
            //System.out.println("CONNECTION URL: " + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();

            //android->server
            OutputStream outs = conn.getOutputStream();
            outs.write(param.getBytes("UTF-8"));
            outs.flush();
            outs.close();

            //server->android
            InputStream is = null;
            BufferedReader in = null;
            String data = "";

            conn.disconnect();
            conn.getInputStream();
            is = conn.getInputStream();
            in = new BufferedReader(new InputStreamReader(is), 8 * 1024);
            String line = null;
            StringBuilder buff = new StringBuilder();
            while ((line = in.readLine()) != null) {
                buff.append(line);
                buff.append("\n");
                break;
            }
            result = buff.toString().trim();
        } catch (MalformedURLException e) {
            System.out.println("MalformedURLException: " + errorMessage);
        } catch (IOException e) {
            System.out.println("IOException: " + errorMessage);
        } catch (NullPointerException e) {
            System.out.println("NullPointerException: " + errorMessage);
        }

        return result;
    }


    private void mappingSubject(Subject subject) {
        if(subject.subjectDiv.contains("전공")) {
            //학부/과 맵이 존재하는지 검사한 후, 없다면 맵을 만든 후 추가 진행
            if(!watchingSubjects.containsKey(subject.subDept)) {
                watchingSubjects.put(subject.subDept, new HashMap<String, Subject>());
            }
            //해당 학부/과의 해당 과목이 존재하는지 검사한 후, 없다면 추가한 후 진행
            if(!watchingSubjects.get(subject.subDept).containsKey(subject.subjectNo + "-" + subject.classDiv)) {
                watchingSubjects.get(subject.subDept).put(subject.subjectNo + "-" + subject.classDiv, subject);
            }
        } else {
            //4개 영역(교선, 교필, ROTC, 교직) 맵이 존재하는지 검사한 후, 없다면 맵을 만든 후 추가 진행
            if(!watchingSubjects.containsKey(convertSubjectDivToCode(subject.subjectDiv))) {
                watchingSubjects.put(convertSubjectDivToCode(subject.subjectDiv), new HashMap<String, Subject>());
            }
            //4개 영역(교선, 교필, ROTC, 교직)의 해당 과목이 존재하는지 검사한 후, 없다면 추가한 후 진행
            if(!watchingSubjects.get(convertSubjectDivToCode(subject.subjectDiv)).containsKey(subject.subjectNo + "-" + subject.classDiv)) {
                watchingSubjects.get(convertSubjectDivToCode(subject.subjectDiv)).put(subject.subjectNo + "-" + subject.classDiv, subject);
            }
        }
    }


    private String convertSubjectDivToCode(String subjectDiv) {
        if(subjectDiv.equals("교양선택")) {
            return "A01";
        } else if(subjectDiv.equals("교양필수")) {
            return "A02";
        } else if(subjectDiv.equals("ROTC")) {
            return "A06";
        } else if(subjectDiv.equals("교직")) {
            return "A07";
        } else {
            return "";
        }
    }

    private void makeWatchingSubjectThread() {

        System.out.println("사이즈 보자: " + watchingSubjects.size());

        Iterator<String> it = watchingSubjects.keySet().iterator();
        while(it.hasNext()) {
            final String key = it.next();
            if(key.length() == 3) { //교양 4분류
                final Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            XMLParseAndDetermine(key, buildURL(key, "", ""));
                            throw new InterruptedException();

                        } catch(InterruptedException e) {
                            System.out.println("스레드 끝");
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                th.start();

                //3초 이내 응답이 없으면 중지시킨다. 스레드가 쌓여서 OOM 이 발생하는 것을 방지
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                            if(th.isAlive()) {
                                th.interrupt();
                            }
                        } catch(InterruptedException e) {
                            System.out.println("응답이 늦어서 중지(교양).");
                        }
                    }
                }).start();
            } else {    //40여개 전공
                System.out.println("스레드 생성");


                final Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //System.out.println("부르는 주소: " + buildURL("", watchingSubjects.get(key).get(watchingSubjects.get(key).keySet().iterator().next()).dept, key));
                            XMLParseAndDetermine(key, buildURL("", watchingSubjects.get(key).get(watchingSubjects.get(key).keySet().iterator().next()).dept, key));
                            throw new InterruptedException();
                        } catch(InterruptedException e) {
                            System.out.println("스레드 끝");
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                th.start();

                //3초 이내 응답이 없으면 중지시킨다. 스레드가 쌓여서 OOM 이 발생하는 것을 방지
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                            if(th.isAlive()) {
                                th.interrupt();
                            }
                        } catch(InterruptedException e) {
                            System.out.println("응답이 늦어서 중지(전공).");
                        }
                    }
                }).start();
            }
        }
    }

    //와이즈에서 가져온 XML 파일을 파싱하고, 그 데이터를 비교하여 빈 자리가 있는지 판단하는 메서드
    private void XMLParseAndDetermine(String key, String url) throws ParserConfigurationException, SAXException, IOException {


        //과목 리스트를 HashMap 으로 저장하여, 조회시 O(1)의 성능을 내도록 한다.
        HashMap<String, Element> fetchedSubjects = new HashMap<String, Element>();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(url);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("list");

        //와이즈 API 에서 가져온 새 과목 리스트(0.5초마다 갱신되는)를 HashMap 으로 구성
        for(int i = 0; i < nList.getLength(); i ++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                fetchedSubjects.put(getTagValue("subject_no", element) + "-" + getTagValue("class_div", element), element);
            }
        }

        //리스트 검사(테스트)
        Iterator testIt = fetchedSubjects.keySet().iterator();
        while(testIt.hasNext()) {
            System.out.println("뭐가 들었어: " + getTagValue("subject_nm", fetchedSubjects.get(testIt.next())));
        }

        //빠른 메모리 해제를 통한 메모리릭 방지
        dbFactory = null;
        dBuilder = null;
        doc = null;
        nList = null;

        //watching 중인 과목들에 대하여 HashMap 을 조회하여 수강인원수 비교
        Iterator it = watchingSubjects.get(key).keySet().iterator();
        while(it.hasNext()) {
            Subject watchingSubject = watchingSubjects.get(key).get(it.next());


            Element fetchedSubject = fetchedSubjects.get(watchingSubject.subjectNo + "-" + watchingSubject.classDiv);
            System.out.println("과목명: " + fetchedSubjects.get(watchingSubject.subjectNo + "-" + watchingSubject.classDiv));
            try {
                int tlsnCount = (getTagValue("tlsn_count", fetchedSubject) == null || getTagValue("tlsn_count", fetchedSubject).equals("")) ? 0 : Integer.parseInt(getTagValue("tlsn_count", fetchedSubject));
                int tlsnLimitCount = (getTagValue("tlsn_limit_count", fetchedSubject) == null || getTagValue("tlsn_limit_count", fetchedSubject).equals("")) ? 0 : Integer.parseInt(getTagValue("tlsn_limit_count", fetchedSubject));

                //이전 상태에 비어있지 않았고, 지금 비어있는 상황이라면 알림을 보낸다.
                if(tlsnCount < tlsnLimitCount) {
                    if(!watchingSubject.previousState) {
                        System.out.println("샌드 푸쉬: " + watchingSubject.getSubjectNo() + "-" + watchingSubject.getClassDiv());
                        sendPush(watchingSubject.getSubjectNo() + "-" + watchingSubject.getClassDiv(), watchingSubject.getSubjectName(), tlsnCount, tlsnLimitCount);
                        watchingSubject.previousState = true;
                    }
                } else {
                    watchingSubject.previousState = false;
                }
            } catch(NullPointerException e) {
                //System.out.println("삭제된 과목");
            }
        }
    }

    //과목 정보를 입력받아 가장 적합한 XML 파일의 주소를 만들어내는 메서드
    private String buildURL(String subjectNo, String classDiv, String subjectDiv, String subjectName, String dept, String subDept) {


        String urlMj = "http://wise.uos.ac.kr/uosdoc/api.ApiUcrMjTimeInq.oapi?apiKey=201902535IGU19211&year=2019&term=A20";
        String urlCult = "http://wise.uos.ac.kr/uosdoc/api.ApiUcrCultTimeInq.oapi?apiKey=201902535IGU19211&year=2019&term=A20";

        /*
        if(subjectDiv.contains("전공")) {
            return urlMj + "&deptDiv=20000&dept=" + dept + "&subDept=" + subDept + "&subjectNo=" + subjectNo + "&classDiv=" + classDiv;
        } else {
            if(subjectDiv.equals("교양선택")) {
                return urlCult + "&subjectDiv=A01&subjectNm=" + subjectName;
            } else if(subjectDiv.equals("교양필수")) {
                return urlCult +"&subjectDiv=A02&subjectNm=" + subjectName;
            } else if(subjectDiv.equals("ROTC")) {
                return urlCult +"&subjectDiv=A06&subjectNm=" + subjectName;
            } else if(subjectDiv.equals("교직")) {
                return urlCult +"&subjectDiv=A07&subjectNm=" + subjectName;
            }
        }
        */

        //return "";
        return "http://dak2183242.cafe24.com/wiv/running_test.xml";
    }

    private String buildURL(String subjectDiv, String dept, String subDept) {
        String urlMj = "http://wise.uos.ac.kr/uosdoc/api.ApiUcrMjTimeInq.oapi?apiKey=201902535IGU19211&year=2019&term=A20";
        String urlCult = "http://wise.uos.ac.kr/uosdoc/api.ApiUcrCultTimeInq.oapi?apiKey=201902535IGU19211&year=2019&term=A20";

        /*
        if(subjectDiv.equals("")) {
            return urlMj + "&deptDiv=20000&dept=" + dept + "&subDept=" + subDept;
        } else {
            return urlCult + "&subjectDiv=" + subjectDiv;
        }
        */

        return "http://dak2183242.cafe24.com/wiv/running_test.xml";


    }

    //"http://wise.uos.ac.kr/uosdoc/api.ApiUcrMjTimeInq.oapi?apiKey=201902535IGU19211&year=2019&term=A20&deptDiv=20000&dept=A200280128&subDept=A200310131"

    //사용자의 기기에 알림을 보내는 메서드
    private void sendPush(String topic, String subjectName, int tlsnCount, int tlsnLimitCount) throws IOException {

        final String apiKey = "AAAAbMh5-sU:APA91bGIzNHvjlDIe2CteU5S-lQgpwh3Fb5bNcBOBkmWHNmxI5TnD8McQucQzzV835zy0EiM3OqemJJ5winquo7ErUnHg9Z3K4ilpFldaClPESva0fJer5D3gfk8UTv6H2aFhs-BguvQ";
        URL url = new URL("https://fcm.googleapis.com/fcm/send");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "key=" + apiKey);

        conn.setDoOutput(true);

        //메시지 내용 작성
        String input = "{\"data\" : {\"title\" : \"빈자리가 생겼습니다!\", \"body\" : \"[" + tlsnCount + "/" + tlsnLimitCount + "] "+ subjectName + " 과목의 빈자리가 생겼습니다.\", \"topic\" : \"" + topic + "\"}, \"to\" : \"/topics/" + topic + "\", \"priority \": \"high\"}";


        OutputStream os = conn.getOutputStream();

        // 서버에서 날려서 한글 깨지는 사람은 아래처럼  UTF-8로 인코딩해서 날려주자
        os.write(input.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        //System.out.println("\nSending 'POST' request to URL : " + url);
        //System.out.println("Post parameters : " + input);
        //System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        // print result
        //System.out.println(response.toString());
    }


    //과목 서브클래스
    class Subject {
        private String subjectNo;
        private String classDiv;
        private String subjectDiv;
        private String subjectName;
        private String dept;
        private String subDept;
        private int numWatchingPeople;
        private int currentPeople;
        private int maxPeople;

        private boolean previousState = false; //false: not vacant, true: vacant


        public boolean hasVacant() {
            return (maxPeople > currentPeople);
        }

        public String getSubjectNo() {
            return subjectNo;
        }

        void setSubjectNo(String subjectNo) {
            this.subjectNo = subjectNo;
        }

        public String getClassDiv() {
            return classDiv;
        }

        void setClassDiv(String classDiv) {
            this.classDiv = classDiv;
        }

        public String getSubjectDiv() {
            return subjectDiv;
        }

        void setSubjectDiv(String subjectDiv) {
            this.subjectDiv = subjectDiv;
        }

        public String getSubjectName() {
            return subjectName;
        }

        void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getDept() {
            return dept;
        }

        void setDept(String dept) {
            this.dept = dept;
        }

        public String getSubDept() {
            return subDept;
        }

        void setSubDept(String subDept) {
            this.subDept = subDept;
        }

        public int getNumWatchingPeople() {
            return numWatchingPeople;
        }

        public void setNumWatchingPeople(int numWatchingPeople) {
            this.numWatchingPeople = numWatchingPeople;
        }

        public int getCurrentPeople() {
            return currentPeople;
        }

        public void setCurrentPeople(int currentPeople) {
            this.currentPeople = currentPeople;
        }

        public int getMaxPeople() {
            return maxPeople;
        }

        public void setMaxPeople(int maxPeople) {
            this.maxPeople = maxPeople;
        }

        public boolean isPreviousState() {
            return previousState;
        }

        public void setPreviousState(boolean previousState) {
            this.previousState = previousState;
        }
    }
}
