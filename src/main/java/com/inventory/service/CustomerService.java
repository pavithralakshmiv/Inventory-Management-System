package com.inventory.service;

import com.inventory.dto.CustomerRequest;
import com.inventory.dto.HistoryRow;
import com.inventory.entity.Customer;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.SaleRepository;
import com.inventory.util.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final SaleRepository saleRepository;
    private final LogService logService;

    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository,
                           SaleRepository saleRepository, LogService logService) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.saleRepository = saleRepository;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public List<Customer> search(String q) {
        return customerRepository.search(q == null ? "" : q.trim());
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        String email = Text.email(request.email());
        if (email != null && customerRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("A customer with email '" + email + "' already exists");
        }
        Customer customer = new Customer();
        apply(customer, request);
        Customer saved = customerRepository.save(customer);
        logService.log("CUSTOMER_ADDED", "Customer added: " + saved.getName());
        return saved;
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        String email = Text.email(request.email());
        if (email != null && customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("A customer with email '" + email + "' already exists");
        }
        apply(customer, request);
        Customer saved = customerRepository.save(customer);
        logService.log("CUSTOMER_UPDATED", "Customer updated: " + saved.getName());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = get(id);
        if (orderRepository.existsByCustomerId(id) || saleRepository.existsByCustomerId(id)) {
            throw new BusinessException("Customer '" + customer.getName() + "' has orders or sales and cannot be deleted. Set the customer to Inactive instead.");
        }
        customerRepository.delete(customer);
        customerRepository.flush();
        logService.log("CUSTOMER_DELETED", "Customer deleted: " + customer.getName());
    }

    @Transactional(readOnly = true)
    public List<HistoryRow> history(Long id) {
        get(id);
        List<HistoryRow> rows = new ArrayList<>();
        orderRepository.findByCustomerIdOrderByIdDesc(id).forEach(o ->
                rows.add(new HistoryRow("Order", o.getOrderNumber(), o.getOrderDate(), o.getTotal(), o.getStatus())));
        saleRepository.findByCustomerIdOrderByIdDesc(id).forEach(s ->
                rows.add(new HistoryRow("Sale", s.getInvoiceNumber(), s.getSaleDate(), s.getTotal(), s.getStatus())));
        rows.sort(Comparator.comparing(HistoryRow::date).reversed());
        return rows;
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setMobile(request.mobile().trim());
        customer.setEmail(Text.email(request.email()));
        customer.setAddress(Text.clean(request.address()));
        customer.setStatus(CategoryService.normalizeStatus(request.status()));
    }
}
