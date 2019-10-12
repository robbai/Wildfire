from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class dhlyckwulfltuzjg(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(2738.29,4624.38,461.21),
				velocity=Vector3(1235.2909,163.821,-1298.511),
				angular_velocity=Vector3(-3.28421,4.73561,-1.66951),
				rotation=Rotator(0.16499881,0.77303046,-2.1473813))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(1795.08,4097.64,17.05),
						velocity=Vector3(2123.741,549.951,0.27099997),
						angular_velocity=Vector3(0.0,-3.0999997E-4,0.062009998),
						rotation=Rotator(-0.016682042,0.2551202,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=86.0),
				1: CarState(
					physics=Physics(
						location=Vector3(396.93,4269.44,116.99),
						velocity=Vector3(1607.8009,-332.251,-15.271),
						angular_velocity=Vector3(1.67921,5.23721,-0.03351),
						rotation=Rotator(0.29586655,2.8294275,3.135169)),
					jumped=True,
					double_jumped=True,
					boost_amount=12.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
