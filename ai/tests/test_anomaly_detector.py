"""
Unit Test Suite for HangOut AI Anomaly Detector.
Verifies semantic contradiction, format spam, and event business rule anomalies.
"""

import pytest
import sys
from pathlib import Path

# Add parent directory to sys.path
sys.path.append(str(Path(__file__).parent.parent))

from anomaly.anomaly_detector import ReviewAnomalyDetector, EventAnomalyDetector

def test_detect_semantic_contradiction_positive_stars_negative_words():
    """Verify that 5-star rating with severe negative text is flagged as an anomaly."""
    res = ReviewAnomalyDetector.verify_review(
        rating=5.0,
        comment="The food was terrible and the waiter was arrogant. Worst dinner of my life!",
        title="Une horreur"
    )
    assert res["is_valid"] is False
    assert res["anomaly_score"] >= 0.90
    assert res["recommended_action"] == "FLAG_FOR_MODERATION"
    assert any("Contradiction Semantique" in a for a in res["anomalies"])

def test_detect_semantic_contradiction_low_stars_positive_words():
    """Verify that 1-star rating with highly positive text is flagged as an anomaly."""
    res = ReviewAnomalyDetector.verify_review(
        rating=1.0,
        comment="Absolutely delicious and amazing food! The staff was friendly and perfection.",
        title="Superbe"
    )
    assert res["is_valid"] is False
    assert res["anomaly_score"] >= 0.90
    assert res["recommended_action"] == "FLAG_FOR_MODERATION"

def test_approve_coherent_positive_review():
    """Verify that a legitimate positive review passes without anomalies."""
    res = ReviewAnomalyDetector.verify_review(
        rating=5.0,
        comment="Nous avons adore le repas. Les plats etaient delicieux et le service impeccable.",
        title="Parfait"
    )
    assert res["is_valid"] is True
    assert res["anomaly_score"] < 0.50
    assert res["recommended_action"] == "APPROVE"

def test_event_overbooking_anomaly():
    """Verify that an event with bookedSpots > availableSpots is rejected."""
    res = EventAnomalyDetector.verify_event({
        "title": "Soiree Jazz",
        "availableSpots": 15,
        "bookedSpots": 20,
        "price": 25.0
    })
    assert res["is_valid"] is False
    assert res["recommended_action"] == "REJECT_EVENT"
    assert any("Sur-reservation" in a for a in res["anomalies"])

def test_event_negative_price_anomaly():
    """Verify that negative price is rejected."""
    res = EventAnomalyDetector.verify_event({
        "title": "Atelier Cuisine",
        "availableSpots": 10,
        "bookedSpots": 5,
        "price": -15.0
    })
    assert res["is_valid"] is False
    assert any("Prix negatif" in a for a in res["anomalies"])
