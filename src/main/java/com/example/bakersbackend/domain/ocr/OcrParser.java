package com.example.bakersbackend.domain.ocr;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OcrParser {

    /**
     * 날짜
     * - 2024. 11. 24.
     * - 2024-11-24
     * - 2024/11/24
     * - 2024년 11월 24일
     * - 2024. 11. 24. - 9:20 (뒤에 시간이 같이 붙어 있어도 허용)
     */
    private static final Pattern DATE_PATTERN =
            Pattern.compile(
                    "(20\\d{2})[^0-9]{0,5}(\\d{1,2})[^0-9]{0,5}(\\d{1,2})",
                    Pattern.UNICODE_CASE
            );

    /**
     * 거리
     * - 9.82 km
     * - 9.82km
     * - 9.82 킬로미터 / 키로미터
     */
    private static final Pattern DISTANCE_PATTERN =
            Pattern.compile(
                    "(\\d+(?:\\.\\d+)?)\\s*(?:km|킬로미터|키로미터)",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );

    /**
     * 페이스
     * - 6'04''
     * - 6'04"
     */
    private static final Pattern PACE_PATTERN =
            Pattern.compile("(\\d+'\\d{2}''?)");

    /**
     * HH:MM(:SS) 형태 시간 전반 (duration / 시작시각 둘 다)
     */
    private static final Pattern TIME_PATTERN =
            Pattern.compile("(\\d{1,2}:\\d{2}(?::\\d{2})?)");

    public OcrResponse parse(List<String> texts) {

        String date = null;
        Double distance = null;
        String pace = null;
        String duration = null;

        // ------ 1차 패스: 날짜 / 거리 / 페이스 ------
        for (String raw : texts) {
            if (raw == null) continue;
            String t = raw.trim();
            if (t.isEmpty()) continue;

            // 날짜
            if (date == null && texts.size() >= 2) {

                String first = texts.get(0).trim();   // 예: "2024."
                String second = texts.get(1).trim();  // 예: "11.24."

                // 1) 연도 패턴 확인
                Matcher y = Pattern.compile("^(20\\d{2})\\.?$").matcher(first);

                // 2) 월/일 패턴 확인
                Matcher md = Pattern.compile("^(\\d{1,2})[./-]?(\\d{1,2})\\.?$").matcher(second);

                if (y.find() && md.find()) {
                    String yyyy = y.group(1);
                    String mm = String.format("%02d", Integer.parseInt(md.group(1)));
                    String dd = String.format("%02d", Integer.parseInt(md.group(2)));

                    date = yyyy + "-" + mm + "-" + dd;
                }
            }

            // 거리
            if (distance == null) {
                Matcher m = DISTANCE_PATTERN.matcher(t);
                if (m.find()) {
                    distance = Double.parseDouble(m.group(1));
                } else if (t.matches("\\d+\\.\\d+")) { // 단위 없이 숫자만 있는 경우
                    distance = Double.parseDouble(t);
                }
            }

            // 페이스 (가급적 '페이스'가 들어 있는 라인 우선)
            if (pace == null && t.contains("페이스")) {
                Matcher m = PACE_PATTERN.matcher(t);
                if (m.find()) {
                    pace = m.group(1);
                }
            }
        }

        // 페이스를 여전히 못 찾았으면 전체 라인에서 한 번 더 탐색
        if (pace == null) {
            for (String raw : texts) {
                if (raw == null) continue;
                String t = raw.trim();
                if (t.isEmpty()) continue;

                Matcher m = PACE_PATTERN.matcher(t);
                if (m.find()) {
                    pace = m.group(1);
                    break;
                }
            }
        }

        // ------ 2차 패스: duration 후보 모두 수집 ------
        class TimeCandidate {
            final String value;   // "59:37"
            final int index;      // texts 상의 인덱스
            final boolean nearDurationLabel; // "시간" 라벨 근처인지 여부

            TimeCandidate(String value, int index, boolean nearDurationLabel) {
                this.value = value;
                this.index = index;
                this.nearDurationLabel = nearDurationLabel;
            }
        }

        List<TimeCandidate> candidates = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String raw = texts.get(i);
            if (raw == null) continue;
            String t = raw.trim();
            if (t.isEmpty()) continue;

            // 날짜/요일/시각 라인으로 보이면 duration 후보에서 제외
            if (t.matches(".*(년|월|일|요일).*")
                    || t.matches(".*(오전|오후|AM|PM).*")
                    || DATE_PATTERN.matcher(t).find()) {
                continue;
            }

            Matcher m = TIME_PATTERN.matcher(t);
            while (m.find()) {
                String timeStr = m.group(1);

                // 앞/뒤 라인 중에 "시간"이 있는지 확인
                boolean nearLabel = false;
                if (t.contains("시간")) {
                    nearLabel = true;
                } else {
                    if (i + 1 < texts.size()) {
                        String next = texts.get(i + 1);
                        if (next != null && next.contains("시간")) {
                            nearLabel = true;
                        }
                    }
                    if (!nearLabel && i - 1 >= 0) {
                        String prev = texts.get(i - 1);
                        if (prev != null && prev.contains("시간")) {
                            nearLabel = true;
                        }
                    }
                }

                candidates.add(new TimeCandidate(timeStr, i, nearLabel));
            }
        }

        // 후보가 있다면 가장 그럴듯한 duration 선택
        if (!candidates.isEmpty()) {
            TimeCandidate best = null;
            int bestScore = Integer.MIN_VALUE;

            for (TimeCandidate c : candidates) {
                int seconds = toSeconds(c.value);

                // 점수 계산
                int score = seconds;

                // "시간" 라벨 근처면 강하게 가산점
                if (c.nearDurationLabel) {
                    score += 1_000_000;
                }

                // 너무 짧은 시간(예: 0~15분)은 시작 시각일 가능성이 크니 약간 감점
                if (seconds < 15 * 60) {
                    score -= 100_000;
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }

            if (best != null) {
                duration = best.value;
            }
        }

        return new OcrResponse(
                date,
                distance,
                pace,
                duration
        );
    }

    /**
     * "MM:SS", "HH:MM", "HH:MM:SS" 형태를 초 단위로 변환
     */
    private int toSeconds(String timeStr) {
        String[] parts = timeStr.split(":");
        try {
            if (parts.length == 2) {
                int m = Integer.parseInt(parts[0]);
                int s = Integer.parseInt(parts[1]);
                return m * 60 + s;
            } else if (parts.length == 3) {
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int s = Integer.parseInt(parts[2]);
                return h * 3600 + m * 60 + s;
            } else {
                return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}