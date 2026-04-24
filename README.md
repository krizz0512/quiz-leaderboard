Quiz Leaderboard System

This project implements a quiz leaderboard system using Java.

Approach:
- Polled API 10 times (0–9)
- Handled duplicate responses using (roundId + participant)
- Aggregated total scores per participant
- Sorted leaderboard in descending order

Final Output:
Diana -> 470
Ethan -> 455
Fiona -> 440
Total Score: 1365

Note:
API responses were unstable (503 errors / inconsistent validation).
However, the computed leaderboard and total score match the expected values correctly.
