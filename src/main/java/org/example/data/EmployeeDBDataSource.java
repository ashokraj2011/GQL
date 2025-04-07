package org.example.data;

import org.example.entity.Employee;
import org.example.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EmployeeDBDataSource implements DBDataSource {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Override
    public String getEntityType() {
        return "Employee";
    }
    
    @Override
    public List<Map<String, Object>> getAllEntities() {
        return employeeRepository.findAll().stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Map<String, Object>> getEntityById(String id) {
        return employeeRepository.findById(Long.parseLong(id))
                .map(this::convertToMap);
    }
    
    @Override
    public List<Map<String, Object>> getEntitiesByField(String fieldName, Object value) {
        // Simple implementation for common fields
        switch (fieldName) {
            case "department":
                return employeeRepository.findByDepartment(value.toString()).stream()
                        .map(this::convertToMap)
                        .collect(Collectors.toList());
            case "position":
                return employeeRepository.findByPositionContaining(value.toString()).stream()
                        .map(this::convertToMap)
                        .collect(Collectors.toList());
            case "salary":
                if (value instanceof Number) {
                    return employeeRepository.findBySalaryGreaterThan(((Number) value).doubleValue()).stream()
                            .map(this::convertToMap)
                            .collect(Collectors.toList());
                }
                break;
            default:
                // For other fields use a dynamic query with Example
                try {
                    Employee example = new Employee();
                    setFieldValue(example, fieldName, value);
                    
                    ExampleMatcher matcher = ExampleMatcher.matching()
                        .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                        .withIgnoreCase();
                    
                    return employeeRepository.findAll(Example.of(example, matcher)).stream()
                            .map(this::convertToMap)
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    System.err.println("Error creating example query: " + e.getMessage());
                }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Set field value on Employee object using reflection
     */
    private void setFieldValue(Employee employee, String fieldName, Object value) throws Exception {
        switch (fieldName) {
            case "firstName":
                employee.setFirstName(value.toString());
                break;
            case "lastName":
                employee.setLastName(value.toString());
                break;
            case "email":
                employee.setEmail(value.toString());
                break;
            case "department":
                employee.setDepartment(value.toString());
                break;
            case "position":
                employee.setPosition(value.toString());
                break;
            case "salary":
                if (value instanceof Number) {
                    employee.setSalary(((Number) value).doubleValue());
                }
                break;
            case "hireDate":
                if (value instanceof String) {
                    employee.setHireDate(LocalDate.parse(value.toString()));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown field: " + fieldName);
        }
    }
    
    /**
     * Convert Employee entity to a Map for GQL API
     */
    private Map<String, Object> convertToMap(Employee employee) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", employee.getId().toString());
        map.put("firstName", employee.getFirstName());
        map.put("lastName", employee.getLastName());
        map.put("email", employee.getEmail());
        map.put("department", employee.getDepartment());
        map.put("position", employee.getPosition());
        
        if (employee.getHireDate() != null) {
            map.put("hireDate", employee.getHireDate().toString());
        }
        
        if (employee.getSalary() != null) {
            map.put("salary", employee.getSalary());
        }
        
        return map;
    }
}
