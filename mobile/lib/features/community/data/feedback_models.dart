/// App feedback categories, mirroring the backend `FeedbackCategory` enum.
enum FeedbackCategory { bug, suggestion, praise, other }

extension FeedbackCategoryWire on FeedbackCategory {
  /// Backend enum name sent in the request body.
  String get wireName {
    switch (this) {
      case FeedbackCategory.bug:
        return 'BUG';
      case FeedbackCategory.suggestion:
        return 'SUGGESTION';
      case FeedbackCategory.praise:
        return 'PRAISE';
      case FeedbackCategory.other:
        return 'OTHER';
    }
  }
}
