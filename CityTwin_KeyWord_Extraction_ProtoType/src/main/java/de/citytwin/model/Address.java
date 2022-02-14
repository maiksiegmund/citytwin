package de.citytwin.model;

public class Address {

    public Address(String str_name, double hnr, String hnr_zusatz) {
        super();
        this.str_name = str_name;
        this.hnr = hnr;
        this.hnr_zusatz = hnr_zusatz;
    }

    public Address(String str_name, double hnr, String hnr_zusatz, String bez_name) {
        super();
        // street name
        this.str_name = str_name;
        // house number
        this.hnr = hnr;
        // additional house number e.g. 4E
        this.hnr_zusatz = hnr_zusatz;
        // section name
        this.bez_name = bez_name;
    }

    private Long fid;
    private String str_name;
    private String addressid;
    private double hnr;
    private String hnr_zusatz;
    private String plz;
    private String bez_name;
    private String bez_nr;
    private String ort_name;
    private String ort_nr;
    private String plr_name;
    private String plr_nr;

    private double latitude;
    private double longitude;

    public Long getFid() {
        return fid;
    }

    public void setFid(Long fid) {
        this.fid = fid;
    }

    public String getName() {
        return str_name;
    }

    public void setStr_name(String str_name) {
        this.str_name = str_name;
    }

    public String getAdressid() {
        return addressid;
    }

    public void setAdressid(String adressid) {
        this.addressid = adressid;
    }

    public double getHnr() {
        return hnr;
    }

    public void setHnr(double hnr) {
        this.hnr = hnr;
    }

    public String getHnr_zusatz() {
        return hnr_zusatz;
    }

    public void setHnr_zusatz(String hnr_zusatz) {
        this.hnr_zusatz = hnr_zusatz;
    }

    public String getPlz() {
        return plz;
    }

    public void setPlz(String plz) {
        this.plz = plz;
    }

    public String getBez_name() {
        return bez_name;
    }

    public void setBez_name(String bez_name) {
        this.bez_name = bez_name;
    }

    public String getBez_nr() {
        return bez_nr;
    }

    public void setBez_nr(String bez_nr) {
        this.bez_nr = bez_nr;
    }

    public String getOrt_name() {
        return ort_name;
    }

    public void setOrt_name(String ort_name) {
        this.ort_name = ort_name;
    }

    public String getOrt_nr() {
        return ort_nr;
    }

    public void setOrt_nr(String ort_nr) {
        this.ort_nr = ort_nr;
    }

    public String getPlr_name() {
        return plr_name;
    }

    public void setPlr_name(String plr_name) {
        this.plr_name = plr_name;
    }

    public String getPlr_nr() {
        return plr_nr;
    }

    public void setPlr_nr(String plr_nr) {
        this.plr_nr = plr_nr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((addressid == null) ? 0 : addressid.hashCode());
        result = prime * result + ((bez_name == null) ? 0 : bez_name.hashCode());
        result = prime * result + ((bez_nr == null) ? 0 : bez_nr.hashCode());
        result = prime * result + ((fid == null) ? 0 : fid.hashCode());
        long temp;
        temp = Double.doubleToLongBits(hnr);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + ((hnr_zusatz == null) ? 0 : hnr_zusatz.hashCode());
        result = prime * result + ((ort_name == null) ? 0 : ort_name.hashCode());
        result = prime * result + ((ort_nr == null) ? 0 : ort_nr.hashCode());
        result = prime * result + ((plr_name == null) ? 0 : plr_name.hashCode());
        result = prime * result + ((plr_nr == null) ? 0 : plr_nr.hashCode());
        result = prime * result + ((plz == null) ? 0 : plz.hashCode());
        result = prime * result + ((str_name == null) ? 0 : str_name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Address other = (Address)obj;
        if (addressid == null) {
            if (other.addressid != null)
                return false;
        } else if (!addressid.equals(other.addressid))
            return false;
        if (bez_name == null) {
            if (other.bez_name != null)
                return false;
        } else if (!bez_name.equals(other.bez_name))
            return false;
        if (bez_nr == null) {
            if (other.bez_nr != null)
                return false;
        } else if (!bez_nr.equals(other.bez_nr))
            return false;
        if (fid == null) {
            if (other.fid != null)
                return false;
        } else if (!fid.equals(other.fid))
            return false;
        if (Double.doubleToLongBits(hnr) != Double.doubleToLongBits(other.hnr))
            return false;
        if (hnr_zusatz == null) {
            if (other.hnr_zusatz != null)
                return false;
        } else if (!hnr_zusatz.equals(other.hnr_zusatz))
            return false;
        if (ort_name == null) {
            if (other.ort_name != null)
                return false;
        } else if (!ort_name.equals(other.ort_name))
            return false;
        if (ort_nr == null) {
            if (other.ort_nr != null)
                return false;
        } else if (!ort_nr.equals(other.ort_nr))
            return false;
        if (plr_name == null) {
            if (other.plr_name != null)
                return false;
        } else if (!plr_name.equals(other.plr_name))
            return false;
        if (plr_nr == null) {
            if (other.plr_nr != null)
                return false;
        } else if (!plr_nr.equals(other.plr_nr))
            return false;
        if (plz == null) {
            if (other.plz != null)
                return false;
        } else if (!plz.equals(other.plz))
            return false;
        if (str_name == null) {
            if (other.str_name != null)
                return false;
        } else if (!str_name.equals(other.str_name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Address [fid=" + fid + ", str_name=" + str_name + ", adressid=" + addressid + ", hnr=" + hnr + ", hnr_zusatz=" + hnr_zusatz + ", plz=" + plz
                + ", bez_name=" + bez_name + ", bez_nr=" + bez_nr + ", ort_name=" + ort_name + ", ort_nr=" + ort_nr + ", plr_name=" + plr_name + ", plr_nr="
                + plr_nr + "]";
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

}
