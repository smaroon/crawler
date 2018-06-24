package com.ge.webcrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CrawlerMain extends Thread {
	
	private static volatile ArrayList <Pages> internet = new ArrayList<Pages>();
	private Pages p;
	private static final String internet1 = "{\"pages\":[{\"address\":\"http://" +
            "foo.bar.com/p1\",\"links\":[\"http://foo.bar.com/p2\",\"http://f" +
            "oo.bar.com/p3\",\"http://foo.bar.com/p4\"]},{\"address\":\"http:" +
            "//foo.bar.com/p2\",\"links\":[\"http://foo.bar.com/p2\",\"http:/" +
            "/foo.bar.com/p4\"]},{\"address\":\"http://foo.bar.com/p4\",\"lin" +
            "ks\":[\"http://foo.bar.com/p5\",\"http://foo.bar.com/p1\",\"http" +
            "://foo.bar.com/p6\"]},{\"address\":\"http://foo.bar.com/p5\",\"l" +
            "inks\":[]},{\"address\":\"http://foo.bar.com/p6\",\"links\":[\"h" +
            "ttp://foo.bar.com/p7\",\"http://foo.bar.com/p4\",\"http://foo.ba" +
            "r.com/p5\"] } ] }";
	private static final String internet2 = "{\"pages\":[{\"address\":\"http://" +
            "foo.bar.com/p1\",\"links\":[\"http://foo.bar.com/p2\"]},{\"addre" +
            "ss\":\"http://foo.bar.com/p2\",\"links\":[\"http://foo.bar.com/p" +
            "3\"]},{\"address\":\"http://foo.bar.com/p3\",\"links\":[\"http:/" +
            "/foo.bar.com/p4\"]},{\"address\":\"http://foo.bar.com/p4\",\"lin" +
            "ks\":[\"http://foo.bar.com/p5\"]},{\"address\":\"http://foo.bar." +
            "com/p5\",\"links\":[\"http://foo.bar.com/p1\"]},{\"address\":\"h" +
            "ttp://foo.bar.com/p6\",\"links\":[\"http://foo.bar.com/p1\"] } ] }";
	
	private final static CopyOnWriteArrayList<String> success = new CopyOnWriteArrayList<String>();
	private final static CopyOnWriteArrayList<String> skip = new CopyOnWriteArrayList<String>();
	private final static CopyOnWriteArrayList<String> error = new CopyOnWriteArrayList<String>();
	
	public CrawlerMain(Pages p) {
		this.p = p;
	}

	/**
	 * Parses the String to a Pages Obj and stores it in the internet 
	 * @param json
	 * @throws IOException
	 */
	public static void parseJson (String json) throws IOException {
		 ObjectMapper mapper = new ObjectMapper();
		 JsonNode node = mapper.readTree(json);
		 JsonNode pageNode = node.get("pages");
			for (JsonNode pn : pageNode) {
				Pages page = mapper.readValue(pn.toString(), Pages.class);
				internet.add(page);
			}
	}
	
	private void addToSkippedSites (String url) {
		boolean found = false;
		Iterator<String> iterator = skip.iterator();
		while(iterator.hasNext()) {
			if(url.equals(iterator.next())) {
				found=true;
				break;
			}
		}
		
		if (found == false) {
			skip.add(url);
		}
	}
	
	private boolean checkIfSiteVisitedAlready (String url) {
		boolean visited = false;
		Iterator<String> iterator = success.iterator();
		while(iterator.hasNext()) {
			if(url.equals(iterator.next())) {
				visited = true;
				addToSkippedSites(url);
				break;
			}
		}
		if (visited == false) {
			success.add(url);
		}
		return visited;
	}
	
	private Pages getNextPage(String url) {
		Pages next = null;
		for (Pages page : internet) {
			if (url.equals(page.getAddress())) {
				next = page;
				break;
			}
		}
		return next;
	}
	
	/**
	 * Prints out the skip results
	 */
	private static void printSkipResults() {
		String printVal = "";
		Iterator<String> skipIter = skip.iterator();
		while(skipIter.hasNext()) {
			printVal += " " + skipIter.next();
		}
		System.out.println("skip: " + printVal);
	}
	
	/** Prints out the success results
	 * 
	 */
	private static void printSuccessResults() {
		String printVal = "";
		Iterator<String> successIter = success.iterator();
		while(successIter.hasNext()) {
			printVal += " " + successIter.next();
		}
		System.out.println("Success: " + printVal);
	}
	
	/**
	 * Prints out the error results
	 */
	private static void printErrorResults () {
		String printVal = "";
		Iterator<String> errorIter = error.iterator();
		while(errorIter.hasNext()) {
			printVal += " " + errorIter.next();
		}
		System.out.println("Error: " + printVal);
	}
	
	@Override
	public void run() {
		try {
			if (!checkIfSiteVisitedAlready(p.getAddress())) {
				String [] links = p.getLinks();
				for (int i = 0; i < links.length; i++) {
					Pages nextPage = getNextPage(links[i]);
					if (nextPage != null) {
						CrawlerMain c = new CrawlerMain(nextPage);
						c.start();
						c.join();
					} else {
						error.add(links[i]);
					}
					
				}
			}
			
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Web Crawler" 													        + "\n" + 
				"*********************************************************************************" + "\n" + 
				"This utility will crawl the urls for each 'page' defined in the supplied internet" + "\n" + 
				"The Internet is defined as JSON test data " 					                    + "\n" + 
				 "********************************************************************************" + "\n");
		System.out.print("Select Internet to Run (1 or 2): ");
		String selectedInternet;
		
		try {
			selectedInternet = br.readLine();
			System.out.println(selectedInternet);
			String jsonToParse = null;
			if (selectedInternet.equals("1")) {
				jsonToParse = internet1;
			} else if (selectedInternet.equals("2")) {
				jsonToParse = internet2;
			} else {
				System.out.println("Invalid input given");
				return;
			}
			
			parseJson(jsonToParse);
			CrawlerMain c = new CrawlerMain(internet.get(0));
			c.start();
			c.join();
			printSuccessResults();
			printSkipResults();
			printErrorResults();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
