from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class nkvjgnbmyecumfsn(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(3989.49,2429.52,192.53),
				velocity=Vector3(-27.470999,-517.66095,-79.101),
				angular_velocity=Vector3(4.67741,1.07971,3.5994098),
				rotation=Rotator(0.8031348,-2.7694106,0.77092123))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(3347.52,1390.15,17.05),
						velocity=Vector3(-549.081,-1298.891,0.26099998),
						angular_velocity=Vector3(0.0,-4.0999998E-4,-1.0999999E-4),
						rotation=Rotator(-0.016682042,-1.9707818,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=46.0),
				1: CarState(
					physics=Physics(
						location=Vector3(3170.66,3257.52,17.01),
						velocity=Vector3(1075.281,-1061.281,0.191),
						angular_velocity=Vector3(-4.0999998E-4,1.0999999E-4,-0.33201),
						rotation=Rotator(-0.00958738,-0.787028,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
