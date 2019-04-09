package es.uma.lcc.neo.cintrano.neotrack.persistence;


public class Spot {

	private int id;

	private double latitude;
	private double longitude;
	private String mac;
	private int status;
    private double data;
    private String type;
    private String unit;
    private String device;


    public Spot() {
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    public double getData() {
        return data;
    }

    public void setData(double data) {
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @Override
	public String toString() {
		return "{" +
				"\"id\":\"" + this.id + "\"," +
				"\"lat\":\"" + this.latitude + "\"," +
				"\"lon\":\"" + this.longitude + "\"," +
                "\"mac\":\"" + this.mac + "\"," +
                "\"data\":\"" + this.data + "\"," +
                "\"type\":\"" + this.type + "\"," +
                "\"unit\":\"" + this.unit + "\"," +
                "\"device\":\"" + this.device + "\"," +
				"\"status\":\"" + this.status + "\"" +
                "}";
	}

}
