package arile.toy.stocksystem.bffserver.stockinfo.client;

import arile.toy.stocksystem.bffserver.stockinfo.dto.ForeignInstitutionTrade;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockInfo;
import arile.toy.stocksystem.bffserver.stockinfo.dto.TradePageResponse;
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
}
