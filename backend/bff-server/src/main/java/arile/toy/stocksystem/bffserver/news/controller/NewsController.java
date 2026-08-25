package arile.toy.stocksystem.bffserver.news.controller;

import arile.toy.stocksystem.bffserver.news.dto.NaverNewsItem;
import arile.toy.stocksystem.bffserver.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public List<NaverNewsItem> getNews(@RequestParam String keyword) {
        return newsService.searchNews(keyword);
    }
}