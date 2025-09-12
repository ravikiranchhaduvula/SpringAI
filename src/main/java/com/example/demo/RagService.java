package com.example.demo;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private final VectorStore store;

    public RagService(VectorStore store) {
        this.store = store;
    }

    /** Ingest plain text into the vector store with an external id and optional metadata. */
    public void ingest(String id, String text, Map<String, Object> metadata) {
        Document d = new Document(text, metadata);
        // keep a useful id in metadata for debugging
        d.getMetadata().put("docId", id);
        store.add(List.of(d));
    }

    /** Retrieve top-k snippets for a query. */
    public List<Document> retrieve(String query, int topK) {
        SearchRequest request = SearchRequest.query(query).withTopK(topK);
        return store.similaritySearch(request);
    }
}
