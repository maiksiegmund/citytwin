package de.citytwin.model;

import de.citytwin.catalog.HasName;

/**
 * @author Maik Siegmund, FH Erfurt
 */
public class Address implements HasName {

    private String strNam;

    private Double hausnr;

    private String hausnrz;

    private String bez_name;

    private String featureId;

    @SuppressWarnings("unused")
    private Address() {
    }

    public Address(String strNam, Double hausnr, String hausnrz) {
        this.strNam = strNam;
        this.hausnr = hausnr;
        this.hausnrz = hausnrz;
        this.bez_name = "";
        this.featureId = "";
    }

    public Address(String strNam, Double hausnr, String hausnrz, String bez_name) {
        this.strNam = strNam;
        this.hausnr = hausnr;
        this.hausnrz = hausnrz;
        this.bez_name = bez_name;
        this.featureId = "";
    }

    public Address(String strNam, Double hausnr, String hausnrz, String bez_name, String featureId) {
        this.strNam = strNam;
        this.hausnr = hausnr;
        this.hausnrz = hausnrz;
        this.bez_name = bez_name;
        this.featureId = featureId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Address other = (Address)obj;
        if (featureId == null) {
            if (other.featureId != null) {
                return false;
            }
        } else if (!featureId.equals(other.featureId)) {
            return false;
        }
        if (bez_name == null) {
            if (other.bez_name != null) {
                return false;
            }
        } else if (!bez_name.equals(other.bez_name)) {
            return false;
        }
        if (hausnr == null) {
            if (other.hausnr != null) {
                return false;
            }
        } else if (!hausnr.equals(other.hausnr)) {
            return false;
        }
        if (hausnrz == null) {
            if (other.hausnrz != null) {
                return false;
            }
        } else if (!hausnrz.equals(other.hausnrz)) {
            return false;
        }
        if (strNam == null) {
            if (other.strNam != null) {
                return false;
            }
        } else if (!strNam.equals(other.strNam)) {
            return false;
        }
        return true;
    }

    public String getBez_name() {
        return bez_name;
    }

    public String getFeatureId() {
        return featureId;
    }

    public Double getHausnr() {
        return hausnr;
    }

    public String getHausnrz() {
        return hausnrz;
    }

    @Override
    public String getName() {
        return strNam;
    }

    public String getStrNam() {
        return strNam;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureId == null) ? 0 : featureId.hashCode());
        result = prime * result + ((bez_name == null) ? 0 : bez_name.hashCode());
        result = prime * result + ((hausnr == null) ? 0 : hausnr.hashCode());
        result = prime * result + ((hausnrz == null) ? 0 : hausnrz.hashCode());
        result = prime * result + ((strNam == null) ? 0 : strNam.hashCode());
        return result;
    }

    public void setBez_name(String bez_name) {
        this.bez_name = bez_name;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public void setHausnr(Double hausnr) {
        this.hausnr = hausnr;
    }

    public void setHausnrz(String hausnrz) {
        this.hausnrz = hausnrz;
    }

    public void setStrNam(String strNam) {
        this.strNam = strNam;
    }

    @Override
    public String toString() {
        return "Address [str_name=" + strNam + ", hnr=" + hausnr + ", hnr_zusatz=" + hausnrz + ", bez_name=" + bez_name + ", feature_id=" + featureId + "]";
    }

}
