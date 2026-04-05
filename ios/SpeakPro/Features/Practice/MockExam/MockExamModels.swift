import Foundation

// MARK: - 完整评测请求

struct FullEvaluateBody: Encodable {
    let audioB64: String
    let referenceText: String
    let examType: String
    let section: String
    let questionId: String?
}

// MARK: - 完整评测结果

struct FullEvaluateResult: Decodable {
    let transcript: String?
    let wordCount: Int?
    let sentenceCount: Int?
    let pronunciationScore: FullPronScore?
    let grammarScore: FullGramScore?
    let contentScore: FullContScore?
    let overallScore: Double?
    let aiFeedback: String?
    let revisedAnswer: RevisedAnswerData?
    let mindMap: MindMapData?
    let keywords: [KeywordItem]?
    let sampleAnswers: [String]?
    let revisedAudioB64: String?
}

struct FullPronScore: Decodable {
    let overall: Double?
    let fluency: Double?
    let integrity: Double?
    let intonation: Double?
    let stress: Double?
}

struct FullGramScore: Decodable {
    let score: Double?
    let errors: [FullGramError]?
    let corrections: [String]?
}

struct FullGramError: Decodable, Identifiable {
    let id = UUID()
    let text: String?
    let type: String?
    let suggestion: String?

    enum CodingKeys: String, CodingKey {
        case text, type, suggestion
    }
}

struct FullContScore: Decodable {
    let score: Double?
    let relevance: Double?
    let vocabulary: Double?
    let coherence: Double?
}

struct RevisedAnswerData: Decodable {
    let text: String?
    let wordCount: Int?
    let sentenceCount: Int?
}

struct MindMapData: Decodable {
    let title: String?
    let children: [MindMapNode]?
}

struct MindMapNode: Decodable, Identifiable {
    let id = UUID()
    let label: String?
    let detail: String?
    let children: [MindMapNode]?

    enum CodingKeys: String, CodingKey {
        case label, detail, children
    }
}

struct KeywordItem: Decodable, Identifiable {
    let id = UUID()
    let word: String?
    let phonetic: String?
    let partOfSpeech: String?
    let definition: String?
    let exampleSentence: String?

    enum CodingKeys: String, CodingKey {
        case word, phonetic, partOfSpeech, definition, exampleSentence
    }
}

// MARK: - 增强的题目得分

struct EnrichedQuestionScore: Identifiable {
    let id = UUID()
    let part: Int
    let question: String
    let score: Double
    let audioURL: URL?
    let fullResult: FullEvaluateResult?
}
