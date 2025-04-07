package org.example.data;

import org.example.entity.Employee;
import org.example.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class EmployeeDataLoader {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
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
    
    /**
     * Get all employees
     */
    public List<Map<String, Object>> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * Get employee by ID
     */
    public Map<String, Object> getEmployeeById(String id) {
        return employeeRepository.findById(Long.parseLong(id))
                .map(this::convertToMap)
                .orElse(null);
    }
    
    /**
     * Get employees by department
     */
    public List<Map<String, Object>> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartment(department).stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * Get employees with salary greater than threshold
     */
    public List<Map<String, Object>> getEmployeesBySalaryGreaterThan(Double threshold) {
        return employeeRepository.findBySalaryGreaterThan(threshold).stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
}
