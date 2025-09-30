package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.PaymentTransaction;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.payload.response.PaymentTransactionStatusResponse;
import com.graduation.schedulingservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


@RequestMapping("/api/payments")
@RequiredArgsConstructor
@RestController
public class PaymentTransactionController {

    private final PaymentTransactionRepository paymentTransactionRepository;

    // API for return the payment transaction based on the order Id
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentTransaction(@PathVariable Long orderId) {

        try {
            Optional<PaymentTransaction> paymentTransaction = paymentTransactionRepository.findByOrderId(String.valueOf(orderId));

            return paymentTransaction.map(transaction -> ResponseEntity.ok(new BaseResponse(1, Constant.RESPONSE_SUCCESS, transaction))).orElseGet(() -> ResponseEntity.ok(new BaseResponse(0, Constant.PAYMENT_NOT_FOUND, null)));


        } catch (Exception e) {
            return ResponseEntity.ok(new BaseResponse(0, e.getClass().getSimpleName(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // API for returning Payment status
    @GetMapping("/order/{orderId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long orderId) {
        try {
            Optional<PaymentTransaction> paymentTransaction = paymentTransactionRepository.findByOrderId(String.valueOf(orderId));

            if (paymentTransaction.isPresent()) {
                return ResponseEntity.ok(new BaseResponse(1, Constant.RESPONSE_SUCCESS, new PaymentTransactionStatusResponse(orderId,String.valueOf(paymentTransaction.get().getStatus()))));
            } else {
                return ResponseEntity.ok(new BaseResponse(0, Constant.PAYMENT_NOT_FOUND, null));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(new BaseResponse(0, e.getClass().getSimpleName(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
}
