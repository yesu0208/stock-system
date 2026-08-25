package arile.toy.stocksystem.bffserver.stockinfo.client;

import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Component
@Slf4j
public class NaverStockCrawlerClient {

    private final RestClient restClient;

    public NaverStockCrawlerClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://finance.naver.com")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .build();
    }

    public StockInfo getStockInfo(String code) {
        try {
            String html = getHtml(code);
            return parse(html);
        } catch (RestClientResponseException e) {
            log.error("Naver stock crawling error. status={}, code={}", e.getStatusCode(), code);
            throw new IllegalStateException("네이버 종목 정보 크롤링 실패", e);
        }
    }

    private String getHtml(String code) {
        return restClient.get()
                .uri("/item/main.naver?code={code}", code)
                .retrieve()
                .body(String.class);
    }

    private StockInfo parse(String html) {
        Document doc = Jsoup.parse(html);

        String marketCap = doc.select("#_market_sum").text();
        String marketCapRank = getValueByTh(doc, "시가총액순위");
        String listedShares = getValueByTh(doc, "상장주식수");

        String[] parTradingSplit = splitBar(getValueByTh(doc, "액면가l매매단위"));
        String foreignLimit = getValueByTh(doc, "외국인한도주식수(A)");
        String foreignOwned = getValueByTh(doc, "외국인보유주식수(B)");
        String foreignRate = getValueByTh(doc, "외국인소진율(B/A)");

        String[] opinionSplit = splitBar(getValueByTh(doc, "투자의견l목표주가"));
        String[] highLowSplit = splitBar(getValueByTh(doc, "52주최고l최저"));
        String[] perEpsSplit = splitBar(getValueByThContains(doc, "PERlEPS"));
        String[] estimatedSplit = splitBar(getValueByThContains(doc, "추정PERlEPS"));
        String[] pbrSplit = splitBar(getValueByThContains(doc, "PBRlBPS"));

        String dividendYield = getValueByThContains(doc, "배당수익률");
        String sameIndustryPer = getValueByTh(doc, "동일업종 PER");
        String sameIndustryRate = getValueByTh(doc, "동일업종 등락률");

        return new StockInfo(
                marketCap, marketCapRank, listedShares,
                parTradingSplit[0], parTradingSplit[1],
                foreignLimit, foreignOwned, foreignRate,
                opinionSplit[0], opinionSplit[1],
                highLowSplit[0], highLowSplit[1],
                perEpsSplit[0], perEpsSplit[1],
                estimatedSplit[0], estimatedSplit[1],
                pbrSplit[0], pbrSplit[1],
                dividendYield,
                sameIndustryPer, sameIndustryRate
        );
    }

    private String getValueByTh(Document doc, String thText) {
        String target = thText.replace(" ", "");
        return findRowValue(doc, thValue -> thValue.contains(target));
    }

    private String getValueByThContains(Document doc, String keyword) {
        return findRowValue(doc, thValue -> thValue.contains(keyword));
    }

    private String findRowValue(Document doc, Predicate<String> matcher) {
        Elements rows = doc.select("tr");

        for (Element row : rows) {
            Element th = row.selectFirst("th");
            Element td = row.selectFirst("td");

            if (th != null && td != null) {
                String thValue = th.text().replace(" ", "").replace("|", "l");
                if (matcher.test(thValue)) {
                    return td.text();
                }
            }
        }
        return "";
    }

    private String[] splitBar(String text) {
        String[] split = text.split("l");
        if (split.length < 2) {
            return new String[]{text, ""};
        }
        return new String[]{split[0].trim(), split[1].trim()};
    }



    public TradePageResponse getForeignInstitutionTrades(String code, int page) {

        List<ForeignInstitutionTrade> current = fetchTradePage(code, page);
        List<ForeignInstitutionTrade> next = fetchTradePage(code, page + 1);

        boolean hasNext = !next.isEmpty() && !isSamePage(current, next);

        return new TradePageResponse(current, hasNext);
    }

    private List<ForeignInstitutionTrade> fetchTradePage(String code, int page) {

        String html;
        try {
            html = restClient.get()
                    .uri("/item/frgn.naver?code={code}&page={page}", code, page)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver foreign trade crawling error. status={}, code={}, page={}",
                    e.getStatusCode(), code, page);
            throw new IllegalStateException("네이버 외국인/기관 매매동향 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        List<ForeignInstitutionTrade> result = new ArrayList<>();

        Elements rows = doc.select("table.type2 tr");

        for (Element row : rows) {

            if (!row.hasAttr("onmouseover")) {
                continue;
            }

            Elements tds = row.select("td");

            if (tds.size() < 9) {
                continue;
            }

            result.add(new ForeignInstitutionTrade(
                    tds.get(0).text(),
                    tds.get(1).text(),
                    parseDiff(tds.get(2).text()),
                    tds.get(3).text(),
                    tds.get(4).text(),
                    tds.get(5).text(),
                    tds.get(6).text(),
                    tds.get(7).text(),
                    tds.get(8).text()
            ));
        }

        return result;
    }

    private boolean isSamePage(List<ForeignInstitutionTrade> a, List<ForeignInstitutionTrade> b) {

        if (a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {

            ForeignInstitutionTrade x = a.get(i);
            ForeignInstitutionTrade y = b.get(i);

            if (!x.date().equals(y.date())
                    || !x.closePrice().equals(y.closePrice())
                    || !x.volume().equals(y.volume())) {
                return false;
            }
        }

        return true;
    }

    private String parseDiff(String text) {

        text = text.replace(",", "").trim();

        if (text.startsWith("상승")) {
            return "▲ " + text.replace("상승", "").trim();
        }
        if (text.startsWith("하락")) {
            return "▼ " + text.replace("하락", "").trim();
        }
        if (text.startsWith("보합0")) {
            return "0";
        }
        if (text.startsWith("상한가")) {
            return "⬆" + text.replace("상한가", "").trim();
        }
        if (text.startsWith("하한가")) {
            return "⬇" + text.replace("하한가", "").trim();
        }

        return text;
    }



    public MarketMainResponse getMarketIndices() {

        Document doc = fetchSiseDocument();

        MarketIndexInfo kospi = parseMarketIndex(doc, "KOSPI", "코스피", false);
        MarketIndexInfo kosdaq = parseMarketIndex(doc, "KOSDAQ", "코스닥", false);
        MarketIndexInfo kospi200 = parseMarketIndex(doc, "KPI200", "코스피200", true);

        return new MarketMainResponse(kospi, kosdaq, kospi200);
    }

    public List<PopularStock> getPopularStocks() {

        Document doc = fetchSiseDocument();

        return parsePopularStocks(doc);
    }

    private Document fetchSiseDocument() {
        try {
            String html = restClient.get()
                    .uri("/sise/")
                    .retrieve()
                    .body(String.class);

            return Jsoup.parse(html);
        } catch (RestClientResponseException e) {
            log.error("Naver market main crawling error. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 증시 메인 크롤링 실패", e);
        }
    }

    private MarketIndexInfo parseMarketIndex(Document doc, String code, String name, boolean isKospi200) {

        String current = doc.select("#" + code + "_now").text();

        Element changeEl = doc.selectFirst("#" + code + "_change");
        String changeText = changeEl.text();

        String direction = "";
        if (!changeEl.select(".nup").isEmpty()) {
            direction = "UP";
        } else if (!changeEl.select(".ndown").isEmpty()) {
            direction = "DOWN";
        }

        String[] split = splitChange(changeText);
        String changeValue = split[0];

        if (direction.equals("UP")) {
            changeValue = "▲ " + changeValue;
        } else if (direction.equals("DOWN")) {
            changeValue = "▼ " + changeValue;
        }

        String changeRate = split[1].replaceAll("(%).*", "$1").trim();

        String timeId = switch (code) {
            case "KOSPI" -> "#time1";
            case "KOSDAQ" -> "#time2";
            default -> "#time3";
        };

        String baseTime = doc.select(timeId).text()
                .replace("장마감", " 장마감")
                .replace("장중", " 장중");

        MarketBreadth breadth;

        if (isKospi200) {
            String basis = doc.select("#kpi200_basis").text()
                    .replace("콘탱고", "")
                    .replace("백워데이션", "")
                    .trim();

            breadth = new MarketBreadth(null, null, null, null, null, basis);
        } else {
            String panelId = code.equals("KOSPI") ? "#tab_sel1_risefall" : "#tab_sel2_risefall";
            Elements stockDds = doc.select(panelId + " dl.stock dd");

            breadth = new MarketBreadth(
                    stockDds.get(0).text(),
                    stockDds.get(1).text(),
                    stockDds.get(2).text(),
                    stockDds.get(3).text(),
                    stockDds.get(4).text(),
                    null
            );
        }

        String trendId = switch (code) {
            case "KOSPI" -> "#tab_sel1_risefall";
            case "KOSDAQ" -> "#tab_sel2_risefall";
            default -> "#tab_sel3_risefall";
        };

        Elements trendDds = doc.select(trendId + " dl.trend dd");

        ProgramTrade programTrade = new ProgramTrade(
                cleanProgramTrade(trendDds.get(0).text()),
                cleanProgramTrade(trendDds.get(1).text()),
                cleanProgramTrade(trendDds.get(2).text())
        );

        String trendSelector = switch (code) {
            case "KOSPI" -> "#tab_sel1_deal_trend";
            case "KOSDAQ" -> "#tab_sel2_deal_trend";
            default -> "#tab_sel3_deal_trend";
        };

        Elements investorItems = doc.select(trendSelector + " li");

        String personal = investorItems.get(1).select(".val").text();
        String foreigner = investorItems.get(2).select(".val").text();
        String institution = investorItems.get(3).select(".val").text();

        InvestorTrend investorTrend = new InvestorTrend(personal, foreigner, institution);

        return new MarketIndexInfo(
                name,
                current,
                changeValue,
                changeRate,
                direction,
                baseTime,
                breadth,
                programTrade,
                investorTrend
        );
    }

    private String cleanProgramTrade(String text) {
        return text
                .replace("비차익 ", "")
                .replace("차익 ", "")
                .replace("전체 ", "")
                .trim();
    }

    private List<PopularStock> parsePopularStocks(Document doc) {

        List<PopularStock> result = new ArrayList<>();

        Elements items = doc.select("#popularItemList li");

        for (Element item : items) {

            Element rankEl = item.select("em").first();
            Element a = item.selectFirst("a");

            if (rankEl == null || a == null) {
                continue;
            }

            int rank = Integer.parseInt(
                    rankEl.text().replace(".", "")
            );

            String href = a.attr("href");
            String code = href.substring(href.indexOf("code=") + 5);
            String name = a.text();

            Element priceEl = item.selectFirst("span.up, span.dn, span.nv, span.noc");

            if (priceEl == null) {
                log.warn("인기 종목 price element를 찾을 수 없음. html={}", item.outerHtml());
                continue;
            }

            String price = priceEl.text();

            Element blindEl = item.selectFirst("span.blind");
            String blindText = blindEl != null ? blindEl.text() : "";

            String direction;

            if (blindText.contains("상한가")) {
                direction = "UPPER_LIMIT";
                price += " ⬆";
            } else if (blindText.contains("하한가")) {
                direction = "LOWER_LIMIT";
                price += " ⬇";
            } else if (blindText.contains("상승")) {
                direction = "UP";
                price += " ▲";
            } else if (blindText.contains("하락")) {
                direction = "DOWN";
                price += " ▼";
            } else if (blindText.contains("보합")) {
                direction = "STEADY";
                price += " -";
            } else {
                direction = "UNKNOWN";
                price += " -";
            }

            result.add(new PopularStock(rank, code, name, price, direction));
        }

        return result;
    }

    private String[] splitChange(String text) {

        String[] tokens = text.split(" ");

        String value = "";
        String rate = "";

        for (String token : tokens) {
            if (token.contains("%")) {
                rate = token;
            } else if (token.matches("^[+-]?[0-9.,]+$")) {
                value = token;
            }
        }

        return new String[]{value, rate};
    }

    public UpjongResponse getAllUpjongs() {

        String html;
        try {
            html = restClient.get()
                    .uri("/sise/sise_group.naver?type=upjong")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 업종 목록 크롤링 실패. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 업종 목록 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        List<UpjongInfo> result = new ArrayList<>();

        Elements rows = doc.select("table.type_1 tr");

        for (Element row : rows) {
            Elements tds = row.select("td");
            if (tds.isEmpty()) {
                continue;
            }

            Element a = tds.get(0).selectFirst("a");
            if (a == null) {
                continue;
            }

            String name = a.text().trim();

            String href = a.attr("href");
            String no = href.contains("no=")
                    ? href.substring(href.indexOf("no=") + 3)
                    : "";

            String changeRate = tds.get(1).text().trim();
            String total = tds.get(2).text().trim();
            String rise = tds.get(3).text().trim();
            String steady = tds.get(4).text().trim();
            String fall = tds.get(5).text().trim();
            String graphRatio = tds.get(6).text().trim();

            result.add(new UpjongInfo(
                    name, no, changeRate, total, rise, steady, fall, graphRatio
            ));
        }

        return new UpjongResponse(result);
    }

    public UpjongStockResponse getUpjongStocks(String upjongNo) {

        String html;
        try {
            html = restClient.get()
                    .uri("/sise/sise_group_detail.naver?type=upjong&no={no}", upjongNo)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 업종별 종목 크롤링 실패. status={}, no={}", e.getStatusCode(), upjongNo);
            throw new IllegalStateException("네이버 업종별 종목 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        List<UpjongStock> result = new ArrayList<>();

        String upjongName = doc.select("h3.sub_tlt").text();

        Elements rows = doc.select("table.type_5 tr");

        for (Element row : rows) {
            Elements tds = row.select("td");
            if (tds.size() < 5) {
                continue;
            }

            Element a = tds.get(0).selectFirst("a");
            if (a == null) {
                continue;
            }

            String name = a.text();

            String href = a.attr("href");
            String code = href.contains("code=")
                    ? href.substring(href.indexOf("code=") + 5)
                    : "";

            String price = tds.get(1).text().trim();
            String change = tds.get(2).text().trim();
            String rate = tds.get(3).text().trim();

            result.add(new UpjongStock(name, code, price, change, rate));
        }

        return new UpjongStockResponse(upjongName, result);
    }
}