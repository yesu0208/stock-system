package arile.toy.stocksystem.bffserver.admin.controller;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.service.AccountCalculator;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import arile.toy.stocksystem.bffserver.exception.admin.AdminUserAccountNotFoundException;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponseMessage;
import arile.toy.stocksystem.bffserver.otoco.repository.BffServerOtocoResponseRepository;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import arile.toy.stocksystem.bffserver.portfolio.service.PortfolioCalculator;
import arile.toy.stocksystem.bffserver.rank.client.RankApiClient;
import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import arile.toy.stocksystem.bffserver.trailingstop.repository.BffServerTrailingStopResponseRepository;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 전용 API. /api/v1/admin/** 는 SecurityConfig에서 ROLE_ADMIN으로 제한
 * 따라서 여기서는 별도의 @AuthenticationPrincipal null 체크나 권한 체크를 하지 않음.
 */
@RestController
@RequestMapping("api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RankApiClient rankApiClient;
    private final AccountCalculator accountCalculator;
    private final PortfolioCalculator portfolioCalculator;
    private final BffServerOrderResponseRepository orderResponseRepository;
    private final BffServerAutoOrderResponseRepository autoOrderResponseRepository;
    private final BffServerOtocoResponseRepository otocoResponseRepository;
    private final BffServerTrailingStopResponseRepository trailingStopResponseRepository;
    private final BffServerAlertResponseRepository alertResponseRepository;

    /** 유저 목록 화면 */
    @GetMapping
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    /** 유저 상세 - 기본 정보 + 랭크 */
    @GetMapping("/{username}")
    public UserDto getUser(@PathVariable String username) {
        UserDto userDto = userService.getUserByUsername(username);
        RankResponse rank = rankApiClient.getRank(username);
        return userDto.withRank(rank);
    }

    /** 유저 상세 - 계좌(현금/평가금액/손익) */
    @GetMapping("/{username}/account")
    public AccountResponse getAccount(@PathVariable String username) {
        try {
            return accountCalculator.calculate(username);
        } catch (RedisAccountNotFoundException e) {
            throw new AdminUserAccountNotFoundException(username);
        }
    }

    /** 유저 상세 - 포트폴리오(업종/종목 비중) */
    @GetMapping("/{username}/portfolio")
    public PortfolioResponse getPortfolio(@PathVariable String username) {
        try {
            return portfolioCalculator.calculate(username);
        } catch (RedisAccountNotFoundException e) {
            throw new AdminUserAccountNotFoundException(username);
        }
    }

    /** 유저 상세 - 실시간 미체결 주문 목록 */
    @GetMapping("/{username}/orders")
    public List<OrderResponseMessage> getOrders(@PathVariable String username) {
        return orderResponseRepository.findAll(username);
    }

    /** 유저 상세 - 실시간 자동주문 목록 */
    @GetMapping("/{username}/auto-orders")
    public List<AutoOrderResponseMessage> getAutoOrders(@PathVariable String username) {
        return autoOrderResponseRepository.findAll(username);
    }

    /** 유저 상세 - 실시간 OTOCO 목록 */
    @GetMapping("/{username}/otocos")
    public List<OtocoResponseMessage> getOtocos(@PathVariable String username) {
        return otocoResponseRepository.findAll(username);
    }

    /** 유저 상세 - 실시간 트레일링스탑 목록 */
    @GetMapping("/{username}/trailing-stops")
    public List<TrailingStopResponseMessage> getTrailingStops(@PathVariable String username) {
        return trailingStopResponseRepository.findAll(username);
    }

    /** 유저 상세 - 등록된 알림 목록 */
    @GetMapping("/{username}/alerts")
    public List<AlertResponseMessage> getAlerts(@PathVariable String username) {
        return alertResponseRepository.findAll(username);
    }
}
