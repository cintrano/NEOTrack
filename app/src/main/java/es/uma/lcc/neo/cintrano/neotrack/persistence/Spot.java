package es.uma.lcc.neo.cintrano.neotrack.persistence;


public class Spot {

	private int id;

	private float latitude;
	private float longitude;
	private String mac;
	private int status;


	public Spot() {
	}

	public float getLatitude() {
		return latitude;
	}

	public void setLatitude(Float latitude) {
		this.latitude = latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public void setLongitude(Float longitude) {
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

	@Override
	public String toString() {
		return "{" +
				"\"id\":\"" + this.id + "\"," +
				"\"latitude\":\"" + this.latitude + "\"," +
				"\"longitude\":\"" + this.longitude + "\"," +
				"\"mac\":\"" + this.mac + "\"," +
				"\"status\":\"" + this.status + "\"" +
                "}";
	}

}
