package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.*; // Import tất cả các DTO thống kê
import com.graduationproject.backend.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List; // Import List
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class StatisticController {

    private final StatisticService statisticService;

    @Autowired
    public StatisticController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    // Các endpoint thống kê ở đây sẽ tự động yêu cầu role ADMIN


    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        DashboardSummaryDTO summary = statisticService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }


    @GetMapping("/revenue/daily")
    public ResponseEntity<List<DailyRevenueDTO>> getDailyRevenueStatistics(
            @RequestParam String from,
            @RequestParam String to) {
        List<DailyRevenueDTO> revenueData = statisticService.getDailyRevenueStatistics(from, to);
        return ResponseEntity.ok(revenueData);
    }


    @GetMapping("/orders/daily")
    public ResponseEntity<List<DailyOrderCountDTO>> getDailyOrderCountStatistics(
            @RequestParam String from,
            @RequestParam String to) {
        List<DailyOrderCountDTO> orderCountData = statisticService.getDailyOrderCountStatistics(from, to);
        return ResponseEntity.ok(orderCountData);
    }


    @GetMapping("/revenue/category")
    public ResponseEntity<List<CategoryRevenueDTO>> getRevenueStatisticsByCategory(
            @RequestParam String from,
            @RequestParam String to) {
        List<CategoryRevenueDTO> revenueData = statisticService.getRevenueStatisticsByCategory(from, to);
        return ResponseEntity.ok(revenueData);
    }


    @GetMapping("/revenue/customer")
    public ResponseEntity<List<CustomerRevenueDTO>> getRevenueStatisticsByCustomer(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "10") int limit) {
        List<CustomerRevenueDTO> revenueData = statisticService.getRevenueStatisticsByCustomer(from, to, limit);
        return ResponseEntity.ok(revenueData);
    }


    @GetMapping("/products/top-selling")
    public ResponseEntity<List<TopProductDTO>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int limit) {
        List<TopProductDTO> topProducts = statisticService.getTopSellingProducts(limit);
        return ResponseEntity.ok(topProducts);
    }


    @GetMapping("/customers/top-completed-orders")
    public ResponseEntity<List<CustomerStatisticDTO>> getTopCustomersByCompletedOrders(
            @RequestParam(defaultValue = "5") int limit) {
        List<CustomerStatisticDTO> topCustomers = statisticService.getTopCustomersByCompletedOrders(limit);
        return ResponseEntity.ok(topCustomers);
    }

    @GetMapping("/customers/top-canceled-orders")
    public ResponseEntity<List<CustomerStatisticDTO>> getTopCustomersByCanceledOrders(
            @RequestParam(defaultValue = "5") int limit) {
        List<CustomerStatisticDTO> topCustomers = statisticService.getTopCustomersByCanceledOrders(limit);
        return ResponseEntity.ok(topCustomers);
    }


    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Integer>> getInventoryStatistics() {
        Map<String, Integer> inventoryData = statisticService.getInventoryStatistics();
        return ResponseEntity.ok(inventoryData);
    }
}