package fastship.model;

/**
 * Address entity (Figure 5 - Class Diagram & Table 25).
 */
public class Address {
    private int addressId;
    private Integer customerId;
    private String contactName;
    private String phone;
    private String street;
    private String ward;
    private String district;
    private String city;
    private String area;

    public Address() {
    }

    public Address(int addressId, Integer customerId, String contactName, String phone,
                   String street, String ward, String district, String city, String area) {
        this.addressId = addressId;
        this.customerId = customerId;
        this.contactName = contactName;
        this.phone = phone;
        this.street = street;
        this.ward = ward;
        this.district = district;
        this.city = city;
        this.area = area;
    }

    public String fullAddress() {
        StringBuilder sb = new StringBuilder();
        if (street != null && !street.isBlank()) {
            sb.append(street.trim());
        }
        if (ward != null && !ward.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward.trim());
        }
        if (district != null && !district.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(district.trim());
        }
        if (city != null && !city.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city.trim());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return fullAddress() + " (" + area + ")";
    }

    public int getAddressId() {
        return addressId;
    }

    public void setAddressId(int addressId) {
        this.addressId = addressId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }
}

